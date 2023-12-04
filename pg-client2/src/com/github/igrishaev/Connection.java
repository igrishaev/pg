package com.github.igrishaev;

import com.github.igrishaev.codec.DecoderTxt;
import com.github.igrishaev.codec.EncoderTxt;
import com.github.igrishaev.enums.*;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.reducer.CljReducer;
import com.github.igrishaev.reducer.IReducer;

import java.io.*;
import java.util.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class Connection implements Closeable {

    private final Config config;
    public final UUID id;
    public final long createdAt;
    private final AtomicInteger aInt;

    private int pid;
    private int secretKey;
    private TXStatus txStatus;
    private Socket socket;
    private BufferedInputStream inStream;
    private BufferedOutputStream outStream;
    private Map<String, String> params;

    private final DecoderTxt decoderTxt;
    private final EncoderTxt encoderTxt;

    public Connection(String host, int port, String user, String password, String database) {
        this(new Config.Builder(user, database)
                .host(host)
                .port(port)
                .password(password)
                .build());
    }

    public Connection(Config config) {
        this.config = config;
        this.params = new HashMap<>();
        this.decoderTxt = new DecoderTxt();
        this.encoderTxt = new EncoderTxt();
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.aInt = new AtomicInteger();
        connect();
    }

    public void close () {
        sendTerminate();
        closeSocket();
    }

    private int nextInt() {
        return aInt.incrementAndGet();
    }

    public synchronized int getPid () {
        return pid;
    }

    public synchronized Boolean isClosed () {
        return socket.isClosed();
    }

    public synchronized TXStatus getTxStatus () {
        return txStatus;
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
                encoderTxt.setEncoding(value);
            case "server_encoding":
                decoderTxt.setEncoding(value);
                break;
            case "DateStyle":
                decoderTxt.setDateStyle(value);
                encoderTxt.setDateStyle(value);
                break;
            case "TimeZone":
                decoderTxt.setTimeZone(value);
                encoderTxt.setTimeZone(value);
                break;
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
        interact(Phase.AUTH, null);
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

        authenticate();
    }

    private void sendMessage (IMessage msg) {
        System.out.println(msg);
        ByteBuffer buf = msg.encode(getClientEncoding());
        System.out.println(Arrays.toString(buf.array()));
        try {
            outStream.write(buf.array());
            outStream.flush();
        }
        catch (IOException e) {
            throw new PGError(e, "could not write bb to the out stream");
        }
    }

    private String generateStatement () {
        return String.format("statement%d", nextInt());
    }

    private String generatePortal () {
        return String.format("portal%d", nextInt());
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

    private void sendCopyData (byte[] buf) {
        sendMessage(new CopyData(buf));
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

        System.out.println(Arrays.toString(bufHeader));

        byte bTag = bbHeader.get();
        int bodySize = bbHeader.getInt() - 4;

        byte[] bufBody = readNBytes(bodySize);
        ByteBuffer bbBody = ByteBuffer.wrap(bufBody);

        System.out.println(Arrays.toString(bufBody));

        return switch ((char) bTag) {
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
            default -> throw new PGError("Unknown message: %s", bTag);
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

    public synchronized Object query(String sql) {
        sendQuery(sql);
        final CljReducer reducer = new CljReducer();
        return interact(Phase.QUERY, reducer).getResults();
    }

    public synchronized PreparedStatement prepare(String sql) {
        return prepare(sql, Collections.emptyList());
    }

    public synchronized <I,V> PreparedStatement prepare (String sql, List<OID> OIDs) {
        String statement = generateStatement();
        Parse parse = new Parse(statement, sql, OIDs);
        sendMessage(parse);
        sendDescribeStatement(statement);
        sendSync();
        sendFlush();
        Result<I,V> res = interact(Phase.PREPARE, null);
        ParameterDescription paramDesc = res.getParameterDescription();
        return new PreparedStatement(parse, paramDesc);
    }

    private void sendBind (String portal, String statement, List<Object> params, OID[] OIDs) {
        Format paramsFormat = config.binaryEncode() ? Format.BIN : Format.TXT;
        Format columnFormat = config.binaryDecode() ? Format.BIN : Format.TXT;
        byte[][] values = new byte[OIDs.length][];
        String encoding = getClientEncoding();
        for (int i = 0; i < OIDs.length; i++) {
            Object param = params.get(i);
            OID oid = OIDs[i];
            switch (paramsFormat) {
                case BIN:
                    throw new PGError("binary encoding is not implemented yet");
                case TXT:
                    String value = encoderTxt.encode(param, oid);
                    try {
                        values[i] = value.getBytes(encoding);
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
                values,
                OIDs,
                paramsFormat,
                columnFormat
        );
        sendMessage(msg);
    }

    public synchronized Object executeStatement (PreparedStatement ps, List<Object> params) {
        String portal = generatePortal();
        String statement = ps.parse().statement();
        OID[] OIDs = ps.parameterDescription().OIDs();
        sendBind(portal, statement, params, OIDs);
        sendDescribePortal(portal);
        sendSync();
        sendFlush();
        return interact(Phase.EXECUTE, new CljReducer()).getResult();
    }

    private <I, R> Result<I, R> interact(Phase phase, IReducer<I, R> reducer) {
        Result<I, R> res = new Result<>(phase, reducer);
        while (true) {
            final Object msg = readMessage();
            System.out.println(msg);
            handleMessage(msg, res);
            if (isEnough(msg, phase)) {
                break;
            }
        }
        res.throwErrorResponse();
        return res;
    }

    private <I,R> void handleMessage(Object msg, Result<I,R> res) {

        switch (msg) {
            case AuthenticationOk ignored:
                break;
            case AuthenticationCleartextPassword ignored:
                handleAuthenticationCleartextPassword();
                break;
            case ParameterStatus x:
                handleParameterStatus(x);
                break;
            case RowDescription x:
                handleRowDescription(x, res);
                break;
            case DataRow x:
                handleDataRow(x, res);
                break;
            case ReadyForQuery x:
                handleReadyForQuery(x);
                break;
            case CommandComplete x:
                handleCommandComplete(x, res);
                break;
            case ErrorResponse x:
                handleErrorResponse(x, res);
                break;
            case BackendKeyData x:
                handleBackendKeyData(x);
                break;
            case ParameterDescription x:
                handleParameterDescription(x, res);
                break;
            case ParseComplete x:
                handleParseComplete(x, res);
                break;

            default: throw new PGError("Cannot handle this message: %s", msg);
        }
    }

    private <I,R> void handleParseComplete(ParseComplete msg, Result<I,R> res) {
        res.setParseComplete(msg);
    }

    private <I,R> void handleParameterDescription (ParameterDescription msg, Result<I,R> res) {
        res.setParameterDescription(msg);
    }

    private void handleAuthenticationCleartextPassword() {
        sendPassword(config.password());
    }

    private void handleParameterStatus(ParameterStatus msg) {
        setParam(msg.param(), msg.value());
    }

    private static <I,R> void handleRowDescription(RowDescription msg, Result<I,R> res) {
        res.setRowDescription(msg);
        short size = msg.columnCount();
        Object[] keys = new Object[size];
        for (short i = 0; i < size; i ++) {
            keys[i] = msg.columns()[i].name();
        }
        res.setCurrentKeys(keys);
    }

    private <I,R> void handleDataRow(DataRow msg, Result<I,R> res) {
        short size = msg.valueCount();
        RowDescription.Column[] cols = res.getRowDescription().columns();
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
                    values[i] = decoderTxt.decode(buf, col.typeOid());
                    break;
                case BIN:
                    throw new PGError("binary decoding is not implemented");
                default:
                    throw new PGError("unknown format: %s", col.format());
            }
        }
        res.setCurrentValues(values);
    }

    private void handleReadyForQuery(ReadyForQuery msg) {
        txStatus = msg.txStatus();
    }

    private static <I, R> void handleCommandComplete(CommandComplete msg, Result<I, R> res) {
        res.setCommandComplete(msg);
    }

    private static <I, R> void handleErrorResponse(ErrorResponse msg, Result<I,R> res) {
        res.addErrorResponse(msg);
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

}