package com.github.igrishaev;

import com.github.igrishaev.auth.MD5;
import com.github.igrishaev.codec.DecoderBin;
import com.github.igrishaev.codec.DecoderTxt;
import com.github.igrishaev.codec.EncoderBin;
import com.github.igrishaev.codec.CodecParams;
import com.github.igrishaev.codec.EncoderTxt;
import com.github.igrishaev.enums.*;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.type.OIDHint;
import com.github.igrishaev.util.IOTool;
import com.github.igrishaev.util.SQL;

import java.io.*;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public synchronized Boolean isClosed () {
        return socket.isClosed();
    }

    public synchronized TXStatus getTxStatus () {
        return txStatus;
    }

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

    public synchronized String getParam (String param) {
        return params.get(param);
    }

    public synchronized Map<String, String> getParams () {
        return Collections.unmodifiableMap(params);
    }

    private void setParam (String param, String value) {
        params.put(param, value);
        switch (param) {
            case "client_encoding":
                codecParams.clientEncoding = param;
                break;
            case "server_encoding":
                codecParams.serverEncoding = param;
                break;
            case "DateStyle":
                codecParams.dateStyle = param;
                break;
            case "TimeZone":
                codecParams.timeZone = param;
                break;
                // TODO: integer_datetimes
        }
    }

    private String getServerEncoding() {
        return params.getOrDefault("server_encoding", Const.UTF8);
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

    private void sendExecute (String portal, Long rowCount) {
        sendMessage(new Execute(portal, rowCount));
    }

    private void sendCopyData (final byte[] buf) {
        sendMessage(new CopyData(buf));
    }
    private void sendCopyData (final byte[] buf, final int size) {
        sendMessage(new CopyData(buf, size));
    }

    private void sendCopyDone () {
        sendMessage(new CopyDone());
    }

    private void sendCopyFail () {
        sendCopyFail(Const.COPY_FAIL_MSG);
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
        sendMessage(new Sync());
    }

    private void sendFlush () {
        sendMessage(new Flush());
    }

    private void sendTerminate () {
        sendMessage(new Terminate());
    }

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
            case 'R' -> AuthenticationResponse.fromByteBuffer(bbBody).parseResponse(bbBody);
            case 'S' -> ParameterStatus.fromByteBuffer(bbBody);
            case 'Z' -> ReadyForQuery.fromByteBuffer(bbBody);
            case 'C' -> CommandComplete.fromByteBuffer(bbBody);
            case 'T' -> RowDescription.fromByteBuffer(bbBody);
            case 'D' -> DataRow.fromByteBuffer(bbBody);
            case 'E' -> ErrorResponse.fromByteBuffer(bbBody);
            case 'K' -> BackendKeyData.fromByteBuffer(bbBody);
            case '1' -> new ParseComplete();
            case '2' -> new BindComplete();
            case '3' -> new CloseComplete();
            case 't' -> ParameterDescription.fromByteBuffer(bbBody);
            case 'H' -> CopyOutResponse.fromByteBuffer(bbBody);
            case 'd' -> CopyData.fromByteBuffer(bbBody);
            case 'c' -> new CopyDone();
            case 'I' -> new EmptyQueryResponse();
            case 'n' -> new NoData();
            case 'v' -> NegotiateProtocolVersion.fromByteBuffer(bbBody);
            case 'A' -> NotificationResponse.fromByteBuffer(bbBody);
            case 'N' -> NoticeResponse.fromByteBuffer(bbBody);
            case 's' -> new PortalSuspended();
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
        return query(sql, ExecuteParams.standard());
    }

    public synchronized Object query(String sql, ExecuteParams executeParams) {
        sendQuery(sql);
        return interact(Phase.QUERY, executeParams).getResult();
    }

    public synchronized PreparedStatement prepare (String sql) {
        return prepare(sql, ExecuteParams.standard());
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

    public Object executeStatement (PreparedStatement stmt) {
        return executeStatement(stmt, ExecuteParams.standard());
    }

    public synchronized Object executeStatement (PreparedStatement stmt,
                                                 ExecuteParams executeParams) {
        String portal = generatePortal();
        sendBind(portal, stmt, executeParams);
        sendDescribePortal(portal);
        sendExecute(portal, executeParams.rowCount());
        sendClosePortal(portal);
        sendSync();
        sendFlush();
        return interact(Phase.EXECUTE, executeParams).getResult();
    }

    public synchronized Object execute (String sql) {
        return execute(sql, ExecuteParams.standard());
    }

    public synchronized Object execute (String sql, List<Object> params) {
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
        acc.throwErrorResponse();
        return acc;
    }

    private Accum interact(Phase phase) {
        return interact(phase, ExecuteParams.standard());
    }

    private void handleMessage(Object msg, Accum acc) {

        // System.out.println(msg);

        switch (msg) {
            case NotificationResponse x:
                handleNotificationResponse(x);
                break;
            case NoData ignored:
                break;
            case EmptyQueryResponse ignored:
                break;
            case CloseComplete ignored:
                break;
            case BindComplete ignored:
                break;
            case AuthenticationOk ignored:
                break;
            case AuthenticationCleartextPassword ignored:
                handleAuthenticationCleartextPassword();
                break;
            case NoticeResponse x:
                handleNoticeResponse(x);
                break;
            case ParameterStatus x:
                handleParameterStatus(x);
                break;
            case RowDescription x:
                handleRowDescription(x, acc);
                break;
            case DataRow x:
                handleDataRow(x, acc);
                break;
            case ReadyForQuery x:
                handleReadyForQuery(x);
                break;
            case PortalSuspended x:
                handlePortalSuspended(x, acc);
                break;
            case AuthenticationMD5Password x:
                handleAuthenticationMD5Password(x);
                break;
            case NegotiateProtocolVersion x:
                handleNegotiateProtocolVersion(x);
                break;
            case CommandComplete x:
                handleCommandComplete(x, acc);
                break;
            case ErrorResponse x:
                handleErrorResponse(x, acc);
                break;
            case BackendKeyData x:
                handleBackendKeyData(x);
                break;
            case ParameterDescription x:
                handleParameterDescription(x, acc);
                break;
            case ParseComplete x:
                handleParseComplete(x, acc);
                break;
            case CopyOutResponse x:
                handleCopyOutResponse(x, acc);
                break;
            case CopyData x:
                handleCopyData(x, acc);
                break;
            case CopyInResponse ignored:
                break;
            case CopyDone ignored:
                break;

            default: throw new PGError("Cannot handle this message: %s", msg);
        }
    }

    private void handlePortalSuspended(PortalSuspended msg, Accum acc) {
        acc.handlePortalSuspended(msg);
    }

    private void handleNotificationResponse(NotificationResponse msg) {
        // TODO: try/catch?
        config.fnNotification().invoke(msg.toClojure());
    }

    private void handleNoticeResponse(NoticeResponse msg) {
        // TODO: try/catch?
        config.fnNotice().invoke(msg.toClojure());
    }

    private void handleNegotiateProtocolVersion(NegotiateProtocolVersion msg) {
        // TODO: print by default?
        config.fnProtocolVersion().invoke(msg.toClojure());
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
        try {
            outputStream.write(msg.bytes());
        } catch (IOException e) {
            throw new PGError(e, "could not handle CopyData response");
        }
    }

    public synchronized Object copyOut (String sql, OutputStream outputStream) {
        ExecuteParams executeParams = ExecuteParams.builder().outputStream(outputStream).build();
        sendQuery(sql);
        Accum acc = interact(Phase.COPY, executeParams);
        return acc.getResult();
    }

    public synchronized Object copyIn (String sql, InputStream inputStream) {
        sendQuery(sql);
        // TODO: prefill the first 5 bytes!!!
        final byte[] buf = new byte[Const.COPY_BUFFER_SIZE];
        while (true) {
            final int size = IOTool.read(inputStream, buf);
            if (size == -1) {
                break;
            }
            sendCopyData(buf, size);
        }
        sendCopyDone();
        return interact(Phase.COPY).getResult();
    }

//    public synchronized Object copyInRows (final String sql, List<List<Object>> params) {
//        return copyInRows(sql, params, new RunParams());
//    }

//    public synchronized Object copyInRows (final String sql, List<List<Object>> params, RunParams runParams) {
//        sendQuery(sql);
//        // TODO: prefill the first 5 bytes!!!
//        for (List<Object> row: params) {
//            int len = row.size();
//            for (Object param: row) {
//                if (runParams.binaryEncode) {
//                    if (param == null) {
//                        -1
//                    }
//                    else {
//                        encoderBin.encode(param, OID.DEFAULT);
//                    }
//                }
//                else {
//                    encoderTxt.encode(param, OID.DEFAULT);
//                }
//            }
//            byte[] bytes = new byte[123];
//            sendCopyData(bytes);
//        }
//        sendCopyDone();
//        return interact(Phase.COPY).getResult();
//    }

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

    private void handleDataRow(DataRow msg, Accum acc) {
        short size = msg.valueCount();
        RowDescription.Column[] cols = acc.getRowDescription().columns();
        ByteBuffer[] bufs = msg.values();
        Object[] values = new Object[size];
        for (short i = 0; i < size; i++) {
            ByteBuffer buf = bufs[i];
            if (buf == null) {
                values[i] = null;
                continue;
            }
            RowDescription.Column col = cols[i];
            switch (col.format()) {
                case TXT:
                    values[i] = DecoderTxt.decode(buf, col.typeOid(), codecParams);
                    break;
                case BIN:
                    values[i] = DecoderBin.decode(buf, col.typeOid(), codecParams);
                    break;
                default:
                    throw new PGError("unknown format: %s", col.format());
            }
        }
        acc.setCurrentValues(values);
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

    public static Connection clone(Connection conn) {
        return new Connection(conn.config);
    }

    public static void cancelRequest(Connection conn) {
        CancelRequest msg = new CancelRequest(Const.CANCEL_CODE, conn.pid, conn.secretKey);
        Connection temp = new Connection(conn.config, false);
        temp.sendMessage(msg);
        temp.close();
    }

    public synchronized void begin () {
        sendQuery("BEGIN");
        interact(Phase.QUERY);
    }

    public synchronized void commit () {
        sendQuery("COMMIT");
        interact(Phase.QUERY);
    }

    public synchronized void rollback () {
        sendQuery("ROLLBACK");
        interact(Phase.QUERY);
    }

    public synchronized boolean isIdle () {
        return txStatus == TXStatus.IDLE;
    }

    public synchronized boolean isTxError () {
        return txStatus == TXStatus.ERROR;
    }

    public synchronized boolean isTransaction () {
        return txStatus == TXStatus.TRANSACTION;
    }

    public void setTxLevel (TxLevel level) {
        sendQuery(SQL.SQLSetTxLevel(level));
        interact(Phase.QUERY);
    }

    public void setTxReadOnly () {
        sendQuery(SQL.SQLSetTxReadOnly);
        interact(Phase.QUERY);
    }

    public synchronized void listen(String channel) {
        query(String.format("listen %s", SQL.quoteChannel(channel)));
    }

    public synchronized void unlisten (String channel) {
        query(String.format("unlisten %s", SQL.quoteChannel(channel)));
    }

    public synchronized void notify (String channel, String message) {
        ArrayList<Object> params = new ArrayList<>(2);
        params.add(channel);
        params.add(message);
        execute("select pg_notify($1, $2)", params);
    }

}
