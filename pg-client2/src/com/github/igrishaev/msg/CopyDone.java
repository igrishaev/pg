package com.github.igrishaev.msg;

public record CopyDone () {
    public static CopyDone INSTANCE = new CopyDone();
    public final static byte[] PAYLOAD = new byte[] {
            (byte)'c',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    };
}
