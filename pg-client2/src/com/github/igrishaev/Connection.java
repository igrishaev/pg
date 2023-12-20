package com.github.igrishaev;

import clojure.lang.Agent;
import clojure.lang.IFn;
import com.github.igrishaev.auth.MD5;
import com.github.igrishaev.codec.DecoderBin;
import com.github.igrishaev.codec.DecoderTxt;
import com.github.igrishaev.codec.EncoderBin;
import com.github.igrishaev.codec.CodecParams;
import com.github.igrishaev.codec.EncoderTxt;
import com.github.igrishaev.copy.Copy;
import com.github.igrishaev.enums.*;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.type.OIDHint;
import com.github.igrishaev.util.IOTool;
import com.github.igrishaev.util.SQL;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class Connection implements Closeable {

    private final Config config;
    private final UUID id;
    private final long createdAt;
    private final AtomicInteger aInt;

    private int pid;
    private int secretKey;
    private TXStatus txStatus;
    private Socket socket;
    private BufferedInputStream inStream;
    private BufferedOutputStream outStream;
    private final Map<String, String> params;
    private final CodecParams codecParams;

    public Connection(String host, int port, String user, String password, String database) {
        this(Config.builder(user, database)
                .host(host)
                .port(port)
                .password(password)
                .build());
    }

    public Connection(Config config, boolean sendStartup) {
        this.config = config;
        this.params = new HashMap<>();
        this.codecParams = CodecParams.standard();
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.aInt = new AtomicInteger();
        connect();
        if (sendStartup) {
            authenticate();
        }
    }

    public Connection(Config config) {
        this(config, true);
    }

    public void close () {
        if (!isClosed()) {
            sendTerminate();
            closeSocket();
        }
    }

    private int nextInt() {
        return aInt.incrementAndGet();
    }

    public synchronized int getPid () {
        return pid;
    }

    public UUID getId() {
        return id;
    }

    @SuppressWarnings("unused")
    public long getCreatedAt() {
        return createdAt;
    }

    public synchronized Boolean isClosed () {
        return socket.isClosed();
    }

    @SuppressWarnings("unused")
    public synchronized TXStatus getTxStatus () {
        return txStatus;
    }

    @SuppressWarnings("unused")
    public synchronized boolean isSSL () {
        return config.useSSL();
    }

    private void closeSocket () {
        try {
            socket.close();
        }
        catch (IOException e) {
            throw new PGError(e, "could not close the socket");
        }
    }

    @SuppressWarnings("unused")
    public synchronized String getParam (String param) {
        return params.get(param);
    }

    @SuppressWarnings("unused")
    public synchronized Map<String, String> getParams () {
        return Collections.unmodifiableMap(params);
    }

    private void setParam (String param, String value) {
        params.put(param, value);
        switch (param) {
            case "client_encoding" ->
                    codecParams.clientCharset = Charset.forName(value);
            case "server_encoding" ->
                    codecParams.serverCharset = Charset.forName(value);
            case "DateStyle" ->
                    codecParams.dateStyle = value;
            case "TimeZone" ->
                    codecParams.timeZone = ZoneId.of(value);
            case "integer_datetimes" ->
                    codecParams.integerDatetime = value.equals("on");
        }
    }

    private String getClientEncoding() {
        return params.getOrDefault("client_encoding", Const.UTF8);
    }

    public Integer getPort () {
        return config.port();
    }

    public String getHost () {
        return config.host();
    }

    public String getUser () {
        return config.user();
    }

    @SuppressWarnings("unused")
    private Map<String, String> getPgParams () {
        return config.pgParams();
    }

    public String getDatabase () {
        return config.database();
    }

    public String toString () {
        return String.format("<PG connection %s@%s:%s/%s>",
                             getUser(),
                             getHost(),
                             getPort(),
                             getDatabase());
    }

    public void authenticate () {
        sendStartupMessage();
        interact(Phase.AUTH);
    }

    private synchronized void connect () {

        final int port = getPort();
        final String host = getHost();

        try {
            socket = new Socket(host, port);
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot connect to a socket");
        }

        try {
            inStream = new BufferedInputStream(
                    socket.getInputStream(),
                    config.inStreamBufSize()
            );
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot get an input stream");
        }

        try {
            outStream = new BufferedOutputStream(
                    socket.getOutputStream(),
                    config.outStreamBufSize()
            );
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot get an output stream");
        }
    }

    private void sendBytes (final byte[] buf) {
        IOTool.write(outStream, buf);
        IOTool.flush(outStream);
    }

    private void sendBytes (final byte[] buf, final int offset, final int len) {
        IOTool.write(outStream, buf, offset, len);
        IOTool.flush(outStream);
    }

    private void sendMessage (IMessage msg) {
        System.out.println(msg);
        ByteBuffer buf = msg.encode(getClientEncoding());
        // System.out.println(Arrays.toString(buf.array()));
        try {
            outStream.write(buf.array());
            outStream.flush();
        }
        catch (IOException e) {
            throw new PGError(e, "could not write bb to the out stream");
        }
    }

    private String generateStatement () {
        return String.format("s%d", nextInt());
    }

    private String generatePortal () {
        return String.format("p%d", nextInt());
    }

    private void sendStartupMessage () {
        StartupMessage msg =
            new StartupMessage(
                    config.protocolVersion(),
                    config.user(),
                    config.database(),
                    config.pgParams()
            );
        sendMessage(msg);
    }

    private void sendCopyData (final byte[] buf) {
        sendMessage(new CopyData(ByteBuffer.wrap(buf)));
    }

    private void sendCopyDone () {
        sendBytes(CopyDone.PAYLOAD);
    }

    private void sendCopyFail (String errorMessage) {
        sendMessage(new CopyFail(errorMessage));
    }

    private void sendQuery (String query) {
        sendMessage(new Query(query));
    }

    private void sendPassword (String password) {
        sendMessage(new PasswordMessage(password));
    }

    private void sendSync () {
        sendBytes(Sync.PAYLOAD);
    }

    private void sendFlush () {
        sendBytes(Flush.PAYLOAD);
    }

    private void sendTerminate () {
        sendBytes(Terminate.PAYLOAD);
    }

    @SuppressWarnings("unused")
    private void sendSSLRequest () {
        sendMessage(new SSLRequest(Const.SSL_CODE));
    }

    private byte[] readNBytes (int len) {
        try {
            return inStream.readNBytes(len);
        }
        catch (IOException e) {
            throw new PGError("Could not read %s byte(s)", len);
        }
    }

    private Object readMessage () {

        byte[] bufHeader = readNBytes(5);
        ByteBuffer bbHeader = ByteBuffer.wrap(bufHeader);

        // System.out.println(Arrays.toString(bufHeader));

        char tag = (char) bbHeader.get();
        int bodySize = bbHeader.getInt() - 4;

        byte[] bufBody = readNBytes(bodySize);
        ByteBuffer bbBody = ByteBuffer.wrap(bufBody);

        // System.out.println(Arrays.toString(bufBody));

        return switch (tag) {
            case 'R' -> AuthenticationResponse.fromByteBuffer(bbBody).parseResponse(bbBody, codecParams.serverCharset);
            case 'S' -> ParameterStatus.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'Z' -> ReadyForQuery.fromByteBuffer(bbBody);
            case 'C' -> CommandComplete.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'T' -> RowDescription.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'D' -> DataRow.fromByteBuffer(bbBody);
            case 'E' -> ErrorResponse.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'K' -> BackendKeyData.fromByteBuffer(bbBody);
            case '1' -> ParseComplete.INSTANCE;
            case '2' -> BindComplete.INSTANCE;
            case '3' -> CloseComplete.INSTANCE;
            case 't' -> ParameterDescription.fromByteBuffer(bbBody);
            case 'H' -> CopyOutResponse.fromByteBuffer(bbBody);
            case 'd' -> CopyData.fromByteBuffer(bbBody);
            case 'c' -> CopyDone.INSTANCE;
            case 'I' -> EmptyQueryResponse.INSTANCE;
            case 'n' -> NoData.INSTANCE;
            case 'v' -> NegotiateProtocolVersion.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'A' -> NotificationResponse.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'N' -> NoticeResponse.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 's' -> PortalSuspended.INSTANCE;
            case 'G' -> CopyInResponse.fromByteBuffer(bbBody);
            default -> throw new PGError("Unknown message: %s", tag);
        };

    }

    private void sendDescribeStatement (String statement) {
        Describe msg = new Describe(SourceType.STATEMENT, statement);
        sendMessage(msg);
    }

    private void sendDescribePortal (String portal) {
        Describe msg = new Describe(SourceType.PORTAL, portal);
        sendMessage(msg);
    }

    private void sendExecute (String portal, long rowCount) {
        Execute msg = new Execute(portal, rowCount);
        sendMessage(msg);
    }

    public synchronized Object query(String sql) {
        return query(sql, ExecuteParams.INSTANCE);
    }

    public synchronized Object query(String sql, ExecuteParams executeParams) {
        sendQuery(sql);
        return interact(Phase.QUERY, executeParams).getResult();
    }

    public synchronized PreparedStatement prepare (String sql, ExecuteParams executeParams) {
        String statement = generateStatement();

        List<OID> OIDsProvided = executeParams.OIDs();
        int OIDsProvidedCount = OIDsProvided.size();

        List<Object> params = executeParams.params();
        int paramCount = params.size();

        OID[] OIDs = new OID[paramCount];

        for (int i = 0; i < paramCount; i++) {
            if (i < OIDsProvidedCount) {
                OIDs[i] = OIDsProvided.get(i);
            }
            else {
                Object param = params.get(i);
                OIDs[i] = OIDHint.guessOID(param);
            }
        }

        Parse parse = new Parse(statement, sql, OIDs);
        sendMessage(parse);
        sendDescribeStatement(statement);
        sendSync();
        sendFlush();
        Accum acc = interact(Phase.PREPARE);
        ParameterDescription paramDesc = acc.getParameterDescription();
        return new PreparedStatement(parse, paramDesc);
    }

    private void sendBind (String portal,
                           PreparedStatement stmt,
                           ExecuteParams executeParams
    ) {
        List<Object> params = executeParams.params();
        OID[] OIDs = stmt.parameterDescription().OIDs();
        int size = params.size();

        if (size != OIDs.length) {
            throw new PGError("Wrong parameters count: %s (must be %s)",
                    size, OIDs.length
            );
        }

        Format paramsFormat = (executeParams.binaryEncode() || config.binaryEncode()) ? Format.BIN : Format.TXT;
        Format columnFormat = (executeParams.binaryDecode() || config.binaryDecode()) ? Format.BIN : Format.TXT;

        byte[][] bytes = new byte[size][];
        String encoding = getClientEncoding();
        String statement = stmt.parse().statement();
        for (int i = 0; i < size; i++) {
            Object param = params.get(i);
            OID oid = OIDs[i];
            switch (paramsFormat) {
                case BIN:
                    ByteBuffer buf = EncoderBin.encode(param, oid, codecParams);
                    bytes[i] = buf.array();
                    break;
                case TXT:
                    String value = EncoderTxt.encode(param, oid, codecParams);
                    try {
                        bytes[i] = value.getBytes(encoding);
                    } catch (UnsupportedEncodingException e) {
                        throw new PGError(e, "could not encode a string, encoding: %s", encoding);
                    }
                    break;
                default:
                    throw new PGError("unknown format: %s", paramsFormat);
            }
        }
        Bind msg = new Bind(
                portal,
                statement,
                bytes,
                OIDs,
                paramsFormat,
                columnFormat
        );
        sendMessage(msg);
    }

    public synchronized Object executeStatement (final PreparedStatement stmt,
                                                 final ExecuteParams executeParams) {
        String portal = generatePortal();
        sendBind(portal, stmt, executeParams);
        sendDescribePortal(portal);
        sendExecute(portal, executeParams.rowCount());
        sendClosePortal(portal);
        sendSync();
        sendFlush();
        return interact(Phase.EXECUTE, executeParams).getResult();
    }

    public synchronized Object execute (final String sql) {
        return execute(sql, ExecuteParams.INSTANCE);
    }

    public synchronized Object execute (final String sql, final List<Object> params) {
        return execute(sql, ExecuteParams.builder().params(params).build());
    }

    public synchronized Object execute (String sql, ExecuteParams executeParams) {
        PreparedStatement stmt = prepare(sql, executeParams);
        Object res = executeStatement(stmt, executeParams);
        closeStatement(stmt);
        return res;
    }

    private void sendCloseStatement (String statement) {
        Close msg = new Close(SourceType.STATEMENT, statement);
        sendMessage(msg);
    }

    private void sendClosePortal (String portal) {
        Close msg = new Close(SourceType.PORTAL, portal);
        sendMessage(msg);
    }

    public synchronized void closeStatement (PreparedStatement statement) {
        closeStatement(statement.parse().statement());
    }

    public synchronized void closeStatement (String statement) {
        sendCloseStatement(statement);
        sendSync();
        sendFlush();
        interact(Phase.CLOSE);
    }

    private Accum interact(Phase phase, ExecuteParams executeParams) {
        Accum acc = new Accum(phase, executeParams);
        while (true) {
            final Object msg = readMessage();
            System.out.println(msg);
            handleMessage(msg, acc);
            if (isEnough(msg, phase)) {
                break;
            }
        }
        acc.maybeThrowError();
        return acc;
    }

    private Accum interact(Phase phase) {
        return interact(phase, ExecuteParams.INSTANCE);
    }

    private void handleMessage(Object msg, Accum acc) {
        // System.out.println(msg);
        switch (msg) {
            case NotificationResponse x ->
                handleNotificationResponse(x);
            case NoData ignored -> {}
            case EmptyQueryResponse ignored -> {}
            case CloseComplete ignored -> {}
            case BindComplete ignored -> {}
            case AuthenticationOk ignored -> {}
            case AuthenticationCleartextPassword ignored ->
                handleAuthenticationCleartextPassword();
            case NoticeResponse x ->
                handleNoticeResponse(x);
            case ParameterStatus x ->
                handleParameterStatus(x);
            case RowDescription x ->
                handleRowDescription(x, acc);
            case DataRow x ->
                handleDataRow(x, acc);
            case ReadyForQuery x ->
                handleReadyForQuery(x);
            case PortalSuspended x ->
                handlePortalSuspended(x, acc);
            case AuthenticationMD5Password x ->
                handleAuthenticationMD5Password(x);
            case NegotiateProtocolVersion x ->
                handleNegotiateProtocolVersion(x);
            case CommandComplete x ->
                handleCommandComplete(x, acc);
            case ErrorResponse x ->
                handleErrorResponse(x, acc);
            case BackendKeyData x ->
                handleBackendKeyData(x);
            case ParameterDescription x ->
                handleParameterDescription(x, acc);
            case ParseComplete x ->
                handleParseComplete(x, acc);
            case CopyOutResponse x ->
                handleCopyOutResponse(x, acc);
            case CopyData x ->
                handleCopyData(x, acc);
            case CopyInResponse ignored ->
                handleCopyInResponse(acc);
            case CopyDone ignored -> {}
            default -> throw new PGError("Cannot handle this message: %s", msg);
        }
    }

    private void handleCopyInResponseStream(Accum acc) {

        final int contentSize = acc.executeParams.copyBufSize();
        final byte[] buf = new byte[5 + contentSize];

        InputStream inputStream = acc.executeParams.inputStream();

        Throwable e = null;
        int read;

        while (true) {
            try {
                read = inputStream.read(buf, 5, contentSize);
            }
            catch (Throwable caught) {
                e = caught;
                break;
            }

            if (read == -1) {
                break;
            }

            ByteBuffer bb = ByteBuffer.wrap(buf);
            bb.put((byte)'d');
            bb.putInt(4 + read);
            sendBytes(buf, 0, 5 + read);
        }

        if (e == null) {
            sendCopyDone();
        }
        else {
            acc.setException(e);
            sendCopyFail(Const.COPY_FAIL_EXCEPTION_MSG);
        }
    }

    private void handleCopyInResponseData (Accum acc, Iterator<List<Object>> iterator) {
        final ExecuteParams executeParams = acc.executeParams;
        final CopyFormat format = executeParams.copyFormat();
        Throwable e = null;

        switch (format) {

            case CSV:
                // TODO: find a way to reduce mem allocation
                String line;
                while (iterator.hasNext()) {
                    try {
                        line = Copy.encodeRowCSV(iterator.next(), executeParams, codecParams);
                    }
                    catch (Throwable caught) {
                        e = caught;
                        break;
                    }
                    sendCopyData(line.getBytes(StandardCharsets.UTF_8));
                }
                break;

            case BIN:
                ByteBuffer buf;
                sendCopyData(Copy.COPY_BIN_HEADER);
                // TODO: find a way to reduce mem allocation
                while (iterator.hasNext()) {
                    try {
                        buf = Copy.encodeRowBin(iterator.next(), executeParams, codecParams);
                    }
                    catch (Throwable caught) {
                        e = caught;
                        break;
                    }
                    sendCopyData(buf.array());
                }
                if (e == null) {
                    sendBytes(Copy.MSG_COPY_BIN_TERM);
                }
                break;

            case TAB:
                e = new PGError("TAB COPY format is not implemented");
                break;
        }

        if (e == null) {
            sendCopyDone();
        }
        else {
            acc.setException(e);
            sendCopyFail(Const.COPY_FAIL_EXCEPTION_MSG);
        }
    }

    private void handleCopyInResponseRows (Accum acc) {
        Iterator<List<Object>> iterator = acc.executeParams.copyInRows()
                .stream()
                .filter(Objects::nonNull)
                .iterator();
        handleCopyInResponseData(acc, iterator);
    }

    private void handleCopyInResponseMaps(Accum acc) {
        List<Object> keys = acc.executeParams.copyMapKeys();
        Iterator<List<Object>> iterator = acc.executeParams.copyInMaps()
                .stream()
                .filter(Objects::nonNull)
                .map(map -> mapToRow(map, keys))
                .iterator();
        handleCopyInResponseData(acc, iterator);
    }

    private void handleCopyInResponse(Accum acc) {

        if (acc.executeParams.copyInRows() != null) {
            handleCopyInResponseRows(acc);
        }
        else if (acc.executeParams.copyInMaps() != null) {
            handleCopyInResponseMaps(acc);
        } else {
            handleCopyInResponseStream(acc);
        }
    }

    private void handlePortalSuspended(PortalSuspended msg, Accum acc) {
        acc.handlePortalSuspended(msg);
    }

    private static void futureCall(IFn f, Object arg) {
        Agent.soloExecutor.submit(() -> {
            f.invoke(arg);
        });
    }

    private void handleNotificationResponse(NotificationResponse msg) {
        futureCall(config.fnNotification(), msg.toClojure());
    }

    private void handleNoticeResponse(NoticeResponse msg) {
        futureCall(config.fnNotice(), msg.toClojure());
    }

    private void handleNegotiateProtocolVersion(NegotiateProtocolVersion msg) {
        futureCall(config.fnProtocolVersion(), msg.toClojure());
    }

    private void handleAuthenticationMD5Password(AuthenticationMD5Password msg) {
        final String hashed = MD5.hashPassword(config.user(), config.password(), msg.salt());
        sendPassword(hashed);
    }

    private void handleCopyOutResponse(CopyOutResponse msg, Accum acc) {
        acc.handleCopyOutResponse(msg);
    }

    private void handleCopyData(CopyData msg, Accum acc) {
        OutputStream outputStream = acc.executeParams.outputStream();
        final byte[] bytes = msg.buf().array();
        try {
            outputStream.write(bytes);
        } catch (Throwable e) {
            acc.setException(e);
            cancelRequest(this);
        }
    }

    public synchronized Object copy (final String sql, final ExecuteParams executeParams) {
        sendQuery(sql);
        Accum acc = interact(Phase.COPY, executeParams);
        return acc.getResult();
    }

    private static List<Object> mapToRow(final Map<?,?> map, final List<Object> keys) {
        final List<Object> row = new ArrayList<>(keys.size());
        for (final Object key: keys) {
            row.add(map.get(key));
        }
        return row;
    }

    private void handleParseComplete(ParseComplete msg, Accum acc) {
        acc.handleParseComplete(msg);
    }

    private void handleParameterDescription (ParameterDescription msg, Accum acc) {
        acc.handleParameterDescription(msg);
    }

    private void handleAuthenticationCleartextPassword() {
        sendPassword(config.password());
    }

    private void handleParameterStatus(ParameterStatus msg) {
        setParam(msg.param(), msg.value());
    }

    private static void handleRowDescription(RowDescription msg, Accum acc) {
        acc.handleRowDescription(msg);
    }

    private void handleDataRowUnsafe(final DataRow msg, final Accum acc) {
        final short size = msg.valueCount();
        final RowDescription.Column[] cols = acc.getRowDescription().columns();
        final ByteBuffer[] bufs = msg.values();
        final Object[] values = new Object[size];
        for (short i = 0; i < size; i++) {
            final ByteBuffer buf = bufs[i];
            if (buf == null) {
                values[i] = null;
                continue;
            }
            final RowDescription.Column col = cols[i];
            final Object value = switch (col.format()) {
                case TXT -> DecoderTxt.decode(buf, col.typeOid(), codecParams);
                case BIN -> DecoderBin.decode(buf, col.typeOid(), codecParams);
            };
            values[i] = value;
        }
        acc.setCurrentValues(values);
    }

    private void handleDataRow(final DataRow msg, final Accum acc) {
        try {
            handleDataRowUnsafe(msg, acc);
        }
        catch (Throwable e) {
            acc.setException(e);
            cancelRequest(this);
        }
    }

    private void handleReadyForQuery(ReadyForQuery msg) {
        txStatus = msg.txStatus();
    }

    private static void handleCommandComplete(CommandComplete msg, Accum acc) {
        acc.handleCommandComplete(msg);
    }

    private static void handleErrorResponse(ErrorResponse msg, Accum acc) {
        acc.addErrorResponse(msg);
    }

    private void handleBackendKeyData(BackendKeyData msg) {
        pid = msg.pid();
        secretKey = msg.secretKey();
    }

    private static Boolean isEnough (Object msg, Phase phase) {
        return switch (msg) {
            case ReadyForQuery ignored -> true;
            case ErrorResponse ignored -> phase == Phase.AUTH;
            default -> false;
        };
    }

    @SuppressWarnings("unused")
    public static Connection clone (Connection conn) {
        return new Connection(conn.config);
    }

    public static void cancelRequest(Connection conn) {
        CancelRequest msg = new CancelRequest(Const.CANCEL_CODE, conn.pid, conn.secretKey);
        Connection temp = new Connection(conn.config, false);
        temp.sendMessage(msg);
        temp.close();
    }

    @SuppressWarnings("unused")
    public synchronized void begin () {
        sendQuery("BEGIN");
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public synchronized void commit () {
        sendQuery("COMMIT");
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public synchronized void rollback () {
        sendQuery("ROLLBACK");
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public synchronized boolean isIdle () {
        return txStatus == TXStatus.IDLE;
    }

    @SuppressWarnings("unused")
    public synchronized boolean isTxError () {
        return txStatus == TXStatus.ERROR;
    }

    @SuppressWarnings("unused")
    public synchronized boolean isTransaction () {
        return txStatus == TXStatus.TRANSACTION;
    }

    @SuppressWarnings("unused")
    public void setTxLevel (TxLevel level) {
        sendQuery(SQL.SQLSetTxLevel(level));
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public void setTxReadOnly () {
        sendQuery(SQL.SQLSetTxReadOnly);
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public synchronized void listen (String channel) {
        query(String.format("listen %s", SQL.quoteChannel(channel)));
    }

    @SuppressWarnings("unused")
    public synchronized void unlisten (String channel) {
        query(String.format("unlisten %s", SQL.quoteChannel(channel)));
    }

    @SuppressWarnings("unused")
    public synchronized void notify (String channel, String message) {
        ArrayList<Object> params = new ArrayList<>(2);
        params.add(channel);
        params.add(message);
        execute("select pg_notify($1, $2)", params);
    }

}
