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
            case AuthenticationOk ignored:
                break;
            case AuthenticationCleartextPassword ignored:
                handleMessage(conn);
                break;
            case ParameterStatus x:
                handleMessage(x, conn);
                break;
            case RowDescription x:
                handleMessage(x, res);
                break;
            case DataRow x:
                handleMessage(x, res);
                break;
            case ReadyForQuery x:
                handleMessage(x, conn);
                break;
            case CommandComplete x:
                handleMessage(x, res);
                break;
            case ErrorResponse x:
                handleMessage(x, res);
                break;

            default: throw new PGError("Cannot handle this message: %s", msg);
        }
    }

    static void handleMessage(Connection conn) {
        conn.sendPassword(conn.getPassword());
    }

    static void handleMessage(ParameterStatus msg, Connection conn) {
        conn.setParam(msg.param(), msg.value());
    }

    static void handleMessage(RowDescription msg, Result res) {
        res.addRowDescription(msg);
    }

    static void handleMessage(DataRow msg, Result res) {
        res.addDataRow(msg);
    }

    static void handleMessage(ReadyForQuery msg, Connection conn) {
        char tag = (char)msg.txStatus();
        Keyword txStatus = switch (tag) {
            case 'I' -> (Keyword.intern("I"));
            case 'E' -> (Keyword.intern("E"));
            case 'T' -> (Keyword.intern("T"));
            default -> throw new PGError("unknown tx status: %s", tag);
        };
        conn.setTxStatus(txStatus);
    }

    static void handleMessage(CommandComplete msg, Result res) {
        res.addCommandComplete(msg);
    }

    static void handleMessage(ErrorResponse msg, Result res) {
        res.addErrorResponse(msg);
    }

}
