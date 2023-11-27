package com.github.igrishaev;

import java.io.Closeable;
import clojure.lang.Keyword;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;


public class Connection implements Closeable {

    private static Keyword KW_PORT = Keyword.intern("port");
    private static Keyword KW_HOST = Keyword.intern("host");
    private static Keyword KW_USER = Keyword.intern("user");
    private static Keyword KW_DB = Keyword.intern("database");
    private static Keyword KW_PASS = Keyword.intern("password");

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

    public Connection(Map<Keyword, Object> cljConfig) {
        config = cljConfig;
        params = new HashMap();

        // TODO
        id = "pg123";
        createdAt = 123123123;

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
        if (port == null) {
            throw new PGError("port is null");
        }
        else {
            return port.intValue();
        }
    }

    public String getHost () {
        return (String) config.get(KW_HOST);
    }

    public String getUser () {
        return (String) config.get(KW_USER);
    }

    public String getPassword () {
        return (String) config.get(KW_PASS);
    }

    public String getDatabase () {
        return (String) config.get(KW_DB);
    }

    public String toString () {
        return String.format(
            "<PG connection %s@%s:%s/%s>",
            getUser(),
            getHost(),
            getPort(),
            getDatabase()
        );
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

    // public void sendParse (String query, Map oids) {
    //     String name = "aaa";
    //     Parse msg = new Parse(name, query, oids);
    //     ByteBuffer buf = msg.encode();
    //     socket.write(buf);
    // }

    // public void sendPassword (String password) {
    //     PasswordMessage msg = new PasswordMessage(password);
    //     ByteBuffer buf = msg.encode();
    //     socket.write(buf);
    // }

    // public void sendFlush () {
    //     Flush msg = new Flush();
    //     ByteBuffer buf  = msg.encode();
    //     socket.write(buf);
    // }

    // public void sendQuery (String sql) {
    //     Query msg = new Query(sql);
    //     ByteBuffer buf  = msg.encode();
    //     socket.write(buf);
    // }

    // public Object readMessage () {

    //     ByteBuffer bbHeader = ByteBuffer.allocate(5);
    //     socket.read(bbHeader);

    //     byte tag = bbHeader.get();
    //     int len = bbHeader.getInt();

    //     ByteBuffer bbBody = ByteBuffer.allocate(len - 4);
    //     socket.read(bbBody);

    //     switch (tag) {

    //     case 1:
    //         return new DataRow(bbBody);

    //     case 2:
    //         return new FooBar(bbBody);

    //     default:
    //         throw new PGError("AAAAAA");
    //     }

    // }

    // public Result query (String sql) {
    //     sendQuery(sql);
    //     return interact();
    // }

    // public handleDataRow(Result result, DataRow msg) {

    // }



    // public Result interact(String phase) {
    //     Object msg;
    //     Result = new Result();

    //     while (true) {
    //         msg = readMessage();
    //         switch (msg) {

    //         case DataRow msg ->
    //             handleDataRow(result, msg);

    //         case RowDescription msg ->
    //             handleRowDescription(result, msg);

    //         }

    //     }

    // }




}
