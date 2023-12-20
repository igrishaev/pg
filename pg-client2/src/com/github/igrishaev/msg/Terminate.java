package com.github.igrishaev.msg;

public record Terminate ()  {
    public final static byte[] PAYLOAD = new byte[] {
            (byte)'X',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    };
}
