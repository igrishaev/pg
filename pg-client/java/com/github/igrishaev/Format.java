package com.github.igrishaev;

public enum Format {
    TXT(0), BIN(1);

    Format(int code) {}

    public static Format ofShort (short code) {
        return switch (code) {
            case 0 -> TXT;
            case 1 -> BIN;
            default -> throw new PGError("wrong format code: %s", code);
        };
    }


}
