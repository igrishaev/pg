package com.github.igrishaev.enums;

public enum TxLevel {

    READ_UNCOMMITTED("READ UNCOMMITTED"),
    READ_COMMITTED("READ COMMITTED"),
    REPEATABLE_READ("REPEATABLE READ"),
    SERIALIZABLE("SERIALIZABLE");

    private final String code;

    TxLevel(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public static TxLevel ofCode (String code) {
        return TxLevel.valueOf(code);
    }
}
