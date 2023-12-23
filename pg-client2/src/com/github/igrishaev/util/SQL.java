package com.github.igrishaev.util;

import com.github.igrishaev.enums.TxLevel;

public class SQL {

    public static String quoteChannel (final String sql) {
        return String.format("\"%s\"", sql.replaceAll("\"", "\"\""));
    }

    public static String SQLSetTxReadOnly = "SET TRANSACTION READ ONLY";

    public static String SQLSetTxLevel (final TxLevel level) {
        return String.format("SET TRANSACTION ISOLATION LEVEL %s", level.getCode());
    }

    public static void main (final String[] args) {
        System.out.println(quoteChannel("aa\"a'aa"));
    }

}
