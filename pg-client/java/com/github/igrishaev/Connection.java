package com.github.igrishaev;

import clojure.lang.Keyword;
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

    private static final Keyword KW_PORT = Keyword.intern("port");
    private static final Keyword KW_HOST = Keyword.intern("host");
    private static final Keyword KW_USER = Keyword.intern("user");
    private static final Keyword KW_DB = Keyword.intern("database");
    private static final Keyword KW_PASS = Keyword.intern("password");
    private static final Keyword KW_PG_PARAMS = Keyword.intern("pg-params");
    private static final Keyword KW_PROTO_VER = Keyword.intern("protocol-version");

    private static final String COPY_FAIL_MSG = "COPY has been interrupted by the client";
    private static final Integer SSL_CODE = 80877103;

    public final String id;
    public final long createdAt;

    private Boolean isSSL = false;
    private int pid;
    private int secretKey;
    private Keyword txStatus;
    private Map<Keyword, Object> config;
    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;
    private Map<String, String> params;

    private final DecoderTxt decoderTxr;

    public void close () {
        sendTerminate();
        closeSocket();
    }

    public Boolean getSSL () {
        return isSSL;
    }

    public Map getConfig () {
        return config;
    }

    public Keyword getTxStatus () {
        return txStatus;
    }

    public void setTxStatus (Keyword status) {
        txStatus = status;
    }

    public void setPrivateKey (int key) {
        secretKey = key;
    }

    public InputStream getInputStream () {
        return inStream;
    }

    public Integer nextID () {
        return RT.nextID();
    }

    public Connection(Map<Keyword, Object> cljConfig) {

        config = cljConfig;
        params = new HashMap<>();

        decoderTxr = new DecoderTxt();

        id = String.format("pg%d", nextID());
        createdAt = System.currentTimeMillis();

        connect();
    }

    public synchronized int getPid () {
        return pid;
    }

    public Boolean isClosed () {
        return socket.isClosed();
    }

    private void closeSocket () {
        try {
            socket.close();
        }
        catch (IOException e) {
            throw new PGError(e, "could not close the socket");
        }
    }

    public String getParam (String param) {
        return params.get(param);
    }

    private void setParam (String param, String value) {
        params.put(param, value);
        switch (param) {
            case "server_encoding":
                decoderTxr.setEncoding(value);
        }
    }

    public Integer getPort () {
        Long port = (Long) config.get(KW_PORT);
        return port.intValue();
    }

    public String getHost () {
        return (String) config.get(KW_HOST);
    }

    public String getUser () {
        return (String) config.get(KW_USER);
    }

    private Integer getProtocolVersion () {
        Long proto = (Long) config.get(KW_PROTO_VER);
        return proto.intValue();
    }

    private Map<String, String> getPgParams () {
        return (Map<String, String>) config.get(KW_PG_PARAMS);
    }

    public String getPassword () {
        return (String) config.get(KW_PASS);
    }

    public String getDatabase () {
        return (String) config.get(KW_DB);
    }

    public String toString () {
        return String.format("<PG connection %s@%s:%s/%s>",
                             getUser(),
                             getHost(),
                             getPort(),
                             getDatabase());
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
            inStream = new BufferedInputStream(socket.getInputStream(), 0xFFFF);
            // inStream = socket.getInputStream();
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot get an input stream");
        }

        try {
            outStream = new BufferedOutputStream(socket.getOutputStream(), 0xFFFF);
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot get an output stream");
        }

        sendStartupMessage();
        interact(Phase.AUTH, null);
    }

    public void sendMessage (IMessage msg) {
        System.out.println(msg);
        ByteBuffer buf = msg.encode("UTF-8"); // TODO
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
            new StartupMessage(getProtocolVersion(),
                               getUser(),
                               getDatabase(),
                               getPgParams());
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
        sendCopyFail(COPY_FAIL_MSG);
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
        sendMessage(new SSLRequest(SSL_CODE));
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
        sendPassword(getPassword());
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
                    values[i] = decoderTxr.decode(buf, col.typeOid());
                case BIN:
                    throw new PGError("binary decoding is not implemented");
            }
        }
        res.setCurrentValues(values);
    }

    private void handleMessage(ReadyForQuery msg) {
        char tag = (char)msg.txStatus();
        txStatus = switch (tag) {
            case 'I' -> (Keyword.intern("I"));
            case 'E' -> (Keyword.intern("E"));
            case 'T' -> (Keyword.intern("T"));
            default -> throw new PGError("unknown tx status: %s", tag);
        };
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
