package com.github.igrishaev.msg;

public record Sync () {
    public final static byte[] PAYLOAD = new byte[] {
            (byte)'S',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    };
}
