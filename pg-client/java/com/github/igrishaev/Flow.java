package com.github.igrishaev;

import clojure.lang.Keyword;

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

    static void handleMessage(ParameterStatus msg, Result res, Connection conn) {
        conn.setParam(msg.param, msg.value);
    }

    static void handleMessage(RowDescription msg, Result res, Connection conn) {
        res.addRowDescription(msg);
    }

    static void handleMessage(DataRow msg, Result res, Connection conn) {
        res.addDataRow(msg);
    }

    static void handleMessage(ReadyForQuery msg, Result res, Connection conn) {

        switch ((char)msg.txStatus) {

            case 'I':
                conn.setTxStatus(Keyword.intern("I"));
                break;

            case 'T':
                conn.setTxStatus(Keyword.intern("T"));
                break;

            case 'E':
                conn.setTxStatus(Keyword.intern("E"));
                break;

            default:
                throw new PGError("unknown tx status: %s", msg.txStatus);

        }

    }

    static void handleMessage(CommandComplete msg, Result res, Connection conn) {
        res.addCommandComplete(msg);
    }


}
