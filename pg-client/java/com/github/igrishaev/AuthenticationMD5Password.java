package com.github.igrishaev;

import java.nio.ByteBuffer;

public record AuthenticationMD5Password (byte[] salt) {
    public static Integer status = 5;
    public AuthenticationMD5Password (ByteBuffer buf) {
        this(BBUtil.getNBytes(buf, 4));
    }

}
