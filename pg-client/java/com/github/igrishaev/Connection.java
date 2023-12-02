package com.github.igrishaev;

import clojure.lang.RT;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

public class Connection implements Closeable {

    private final Config config;
    public final String id;
    public final long createdAt;

    private int pid;
    private int secretKey;
    private TXStatus txStatus;
    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;
    private Map<String, String> params;

    private final DecoderTxt decoderTxt;

    public void close () {
        sendTerminate();
        closeSocket();
    }

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
        this.id = String.format("pg%d", nextID());
        this.createdAt = System.currentTimeMillis();
        connect();
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

    private Integer nextID () {
        return RT.nextID();
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

    private void setParam (String param, String value) {
        String paramLower = param.toLowerCase();
        params.put(paramLower, value);
        switch (paramLower) {
            // client_encoding
            case "server_encoding":
                decoderTxt.setEncoding(value);
            case "datestyle":
                decoderTxt.setDateStyle(value);
            case "timezone":
                decoderTxt.setTimeZone(value);
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

    private void authenticate () {
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

    public void sendMessage (IMessage msg) {
        System.out.println(msg);
        ByteBuffer buf = msg.encode(getClientEncoding());
        try {
            outStream.write(buf.array());
        }
        catch (IOException e) {
            throw new PGError(e, "could not write bb to the out stream");
        }
    }

    private String generateStatement () {
        return String.format("statement%d", nextID());
    }

    private String generatePortal () {
        return String.format("portal%d", nextID());
    }

    public void sendParse (String statement, String query, List<Long> OIDs) {
        sendMessage(new Parse(statement, query, OIDs));
    }

    public void sendStartupMessage () {
        StartupMessage msg =
            new StartupMessage(
                    config.protocolVersion(),
                    config.user(),
                    config.database(),
                    config.pgParams()
            );
        sendMessage(msg);
    }

    public void sendExecute (String portal, Long rowCount) {
        sendMessage(new Execute(portal, rowCount));
    }

    public void sendCopyData (byte[] buf) {
        sendMessage(new CopyData(buf));
    }

    public void sendCopyDone () {
        sendMessage(new CopyDone());
    }

    public void sendCopyFail () {
        sendCopyFail(Const.COPY_FAIL_MSG);
    }

    public void sendCopyFail (String errorMessage) {
        sendMessage(new CopyFail(errorMessage));
    }

    public void sendQuery (String query) {
        sendMessage(new Query(query));
    }

    public void sendPassword (String password) {
        sendMessage(new PasswordMessage(password));
    }

    public void sendSync () {
        sendMessage(new Sync());
    }

    public void sendFlush () {
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

        byte bTag = bbHeader.get();
        int bodySize = bbHeader.getInt() - 4;

        byte[] bufBody = readNBytes(bodySize);
        ByteBuffer bbBody = ByteBuffer.wrap(bufBody);

        return switch ((char) bTag) {
            case 'R' -> AuthenticationResponse.fromByteBuffer(bbBody).parseResponse(bbBody);
            case 'S' -> ParameterStatus.fromByteBuffer(bbBody);
            case 'Z' -> ReadyForQuery.fromByteBuffer(bbBody);
            case 'C' -> CommandComplete.fromByteBuffer(bbBody);
            case 'T' -> RowDescription.fromByteBuffer(bbBody);
            case 'D' -> DataRow.fromByteBuffer(bbBody);
            case 'E' -> ErrorResponse.fromByteBuffer(bbBody);
            case 'K' -> BackendKeyData.fromByteBuffer(bbBody);
            default -> throw new PGError("Unknown message: %s", bTag);
        };

    }

    public synchronized Object query(String sql) {
        sendQuery(sql);
        final CljReducer reducer = new CljReducer();
        return interact(Phase.QUERY, reducer).getResults();
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
                handleMessage();
                break;
            case ParameterStatus x:
                handleMessage(x);
                break;
            case RowDescription x:
                handleMessage(x, res);
                break;
            case DataRow x:
                handleMessage(x, res);
                break;
            case ReadyForQuery x:
                handleMessage(x);
                break;
            case CommandComplete x:
                handleMessage(x, res);
                break;
            case ErrorResponse x:
                handleMessage(x, res);
                break;
            case BackendKeyData x:
                handleMessage(x);
                break;

            default: throw new PGError("Cannot handle this message: %s", msg);
        }
    }

    private void handleMessage() {
        sendPassword(config.password());
    }

    private void handleMessage(ParameterStatus msg) {
        setParam(msg.param(), msg.value());
    }

    static <I,R> void handleMessage(RowDescription msg, Result<I,R> res) {
        res.setRowDescription(msg);
        short size = msg.columnCount();
        Object[] keys = new Object[size];
        for (short i = 0; i < size; i ++) {
            keys[i] = msg.columns()[i].name();
        }
        res.setCurrentKeys(keys);
    }

    private <I,R> void handleMessage(DataRow msg, Result<I,R> res) {
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
                case BIN:
                    throw new PGError("binary decoding is not implemented");
            }
        }
        res.setCurrentValues(values);
    }

    private void handleMessage(ReadyForQuery msg) {
        txStatus = msg.txStatus();
    }

    static <I, R> void handleMessage(CommandComplete msg, Result<I, R> res) {
        res.setCommandComplete(msg);
    }

    static <I, R> void handleMessage(ErrorResponse msg, Result<I,R> res) {
        res.addErrorResponse(msg);
    }

    public void handleMessage(BackendKeyData msg) {
        pid = msg.pid();
        secretKey = msg.secretKey();
    }

    static Boolean isEnough (Object msg, Phase phase) {
        return switch (msg) {
            case ReadyForQuery ignored -> true;
            case ErrorResponse ignored -> phase == Phase.AUTH;
            default -> false;
        };
    }

}
