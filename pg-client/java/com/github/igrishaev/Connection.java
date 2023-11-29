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


public class Connection implements Closeable {

    private static Keyword KW_PORT = Keyword.intern("port");
    private static Keyword KW_HOST = Keyword.intern("host");
    private static Keyword KW_USER = Keyword.intern("user");
    private static Keyword KW_DB = Keyword.intern("database");
    private static Keyword KW_PASS = Keyword.intern("password");
    private static Keyword KW_PG_PARAMS = Keyword.intern("pg-params");
    private static Keyword KW_PROTO_VER = Keyword.intern("protocol-version");

    private static String COPY_FAIL_MSG = "COPY has been interrupted by the client";
    private static Integer SSL_CODE = 80877103;

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

    // private Map<String, Object> state;
    // private Map<String, Object> opt;

    // public void sendMessage(Message message) {
    //     bytes byte[] = message.encode();
    //     out_stream.write(bytes);
    // }

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

    public int getPrivateKey () {
        return secretKey;
    }

    public OutputStream getOutputStream () {
        return outStream;
    }

    public InputStream getInputStream () {
        return inStream;
    }

    public Integer nextID () {
        return RT.nextID();
    }

    public Connection(Map<Keyword, Object> cljConfig) {

        config = cljConfig;
        params = new HashMap();

        id = String.format("pg%d", nextID());
        createdAt = System.currentTimeMillis();

        connect();
    }

    public int getPid () {
        return pid;
    }

    public void setPid (int pid) {
        this.pid = pid;
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

    public void setParam (String param, String value) {
        params.put(param, value);
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

    private void connect () {

        Integer port = getPort();
        String host = getHost();

        try {
            socket = new Socket(host, port, true);
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot connect to a socket");
        }

        try {
            inStream = socket.getInputStream();
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot get an input stream");
        }

        try {
            outStream = socket.getOutputStream();
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot get an output stream");
        }

    }

    public void sendMessage (AMessage message) {
        ByteBuffer buf = message.encode("UTF-8"); // TODO
        try {
            outStream.write(buf.array());
        }
        catch (IOException e) {
            throw new PGError(e, "could not write bb to the out stream");
        }
    }

    public String generateStatement () {
        return String.format("statement%d", nextID());
    }

    public String generatePortal () {
        return String.format("portal%d", nextID());
    }

    public void sendParse (String statement, String query, List<Long> oids) {
        sendMessage(new Parse(statement, query, oids));
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

    public void sendTerminate () {
        sendMessage(new Terminate());
    }

    public void sendSSLRequest () {
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

    public Object readMessage () {

        byte[] bufHeader = readNBytes(5);
        ByteBuffer bbHeader = ByteBuffer.wrap(bufHeader);

        byte bTag = bbHeader.get();
        Integer bodySize = bbHeader.getInt() - 4;

        byte[] bufBody = readNBytes(bodySize);
        ByteBuffer bbBody = ByteBuffer.wrap(bufBody);

        switch ((char) bTag) {

        case 'R':
            AuthenticationResponse authResp = new AuthenticationResponse(bbBody);

            switch (authResp.status) {

            case  0: return new AuthenticationOk();
            case  3: return new AuthenticationCleartextPassword();
            case  5: return new AuthenticationMD5Password(bbBody);
            case 10: return new AuthenticationSASL(bbBody);

            default:
                throw new PGError("Unknown auth response message: %s, status: %s",
                                  bTag, authResp.status);
            }

        default:
            throw new PGError("Unknown message: %s", bTag);
        }

    }

    public void handleMessage(Object msg, Result res) {
        throw new PGError("Cannot handle a message: %s", msg);
    }

    public void handleMessage(AuthenticationOk msg, Result res) {
    }

    public void handleMessage(AuthenticationCleartextPassword msg, Result res) {
        sendPassword(getPassword());
    }

    public Result interact(String phase) {
        Result res = new Result(phase, null);

        while (true) {
            Object msg = readMessage();
            handleMessage(msg, res);
            break;
        }

        return res;
    }

}
