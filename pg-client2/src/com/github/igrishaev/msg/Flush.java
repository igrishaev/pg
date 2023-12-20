package com.github.igrishaev.msg;

public record Flush () {
    public final static byte[] PAYLOAD = new byte[] {
            (byte)'H',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    };
}
