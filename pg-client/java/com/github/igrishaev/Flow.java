package com.github.igrishaev;

import clojure.lang.Keyword;

public class Flow {

    static Boolean isEnough (Object msg, String phase) {
        return switch (msg) {
            case ReadyForQuery ignored -> true;
            case ErrorResponse ignored -> phase.equals("auth");
            default -> false;
        };
    }

    public static Object interact(Connection conn, String phase) {
        CljReducer reducer = new CljReducer();
        Result res = new Result(phase, reducer);
        while (true) {
            Object msg = conn.readMessage();
            System.out.println(msg);
            handleMessage(msg, res, conn);
            if (isEnough(msg, phase)) {
                break;
            }
        }
        return res.getResults();
    }

    static void handleMessage(Object msg, Result res, Connection conn) {

        switch (msg) {
            case AuthenticationOk x:
                handleMessage(x, res, conn);
                break;
            case AuthenticationCleartextPassword x:
                handleMessage(x, res, conn);
                break;
            case ParameterStatus x:
                handleMessage(x, res, conn);
                break;
            case RowDescription x:
                handleMessage(x, res, conn);
                break;
            case DataRow x:
                handleMessage(x, res, conn);
                break;
            case ReadyForQuery x:
                handleMessage(x, res, conn);
                break;
            case CommandComplete x:
                handleMessage(x, res, conn);
                break;
            case ErrorResponse x:
                handleMessage(x, res, conn);
                break;

            default: throw new PGError("Cannot handle this message: %s", msg);
        }
    }

    static void handleMessage(AuthenticationOk msg, Result res, Connection conn) {

    }

    static void handleMessage(AuthenticationCleartextPassword msg, Result res, Connection conn) {
        conn.sendPassword(conn.getPassword());
    }

    static void handleMessage(ParameterStatus msg, Result res, Connection conn) {
        conn.setParam(msg.param(), msg.value());
    }

    static void handleMessage(RowDescription msg, Result res, Connection conn) {
        res.addRowDescription(msg);
    }

    static void handleMessage(DataRow msg, Result res, Connection conn) {
        res.addDataRow(msg);
    }

    static void handleMessage(ReadyForQuery msg, Result res, Connection conn) {

        switch ((char)msg.txStatus()) {

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
                throw new PGError("unknown tx status: %s", msg.txStatus());

        }

    }

    static void handleMessage(CommandComplete msg, Result res, Connection conn) {
        res.addCommandComplete(msg);
    }

    static void handleMessage(ErrorResponse msg, Result res, Connection conn) {
        res.addErrorResponse(msg);
    }

}
