package com.github.igrishaev.enums;

public enum SourceType {
    STATEMENT('S'), PORTAL('P');

    private final char code;

    SourceType(char code) {
        this.code = code;
    }

    public char getCode() {
        return this.code;
    }
}
