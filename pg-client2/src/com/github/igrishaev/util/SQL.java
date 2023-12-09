package com.github.igrishaev.util;

import com.github.igrishaev.enums.TxLevel;

public class SQL {

    public static String SQLSetTxReadOnly = "SET TRANSACTION READ ONLY";

    public static String SQLSetTxLevel (TxLevel level) {
        return String.format("SET TRANSACTION ISOLATION LEVEL %s", level.getCode());
    }

}
