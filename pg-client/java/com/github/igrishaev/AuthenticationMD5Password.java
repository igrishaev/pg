package com.github.igrishaev;

import java.nio.ByteBuffer;

public record AuthenticationMD5Password (byte[] salt) {
    public static Integer status = 5;
    public static AuthenticationMD5Password fromByteBuffer(ByteBuffer buf) {
        byte[] salt = new byte[4];
        buf.get(salt);
        return new AuthenticationMD5Password(salt);
    }
}
