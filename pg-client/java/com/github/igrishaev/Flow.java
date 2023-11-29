package com.github.igrishaev;


public class Flow {

    static Object interact(Connection conn, String phase) {

        Result res = new Result(phase, null);

        while (true) {
            Object msg = conn.readMessage();
            handleMessage(msg, res, conn);
            break;
        }

        return res.getResults();
    }

    static void handleMessage(Object msg, Result res, Connection conn) {
        throw new PGError("Cannot handle this message: %s", msg);
    }

    static void handleMessage(AuthenticationOk msg, Result res, Connection conn) {

    }

    static void handleMessage(AuthenticationCleartextPassword msg, Result res, Connection conn) {
        conn.sendPassword(conn.getPassword());
    }

}
