package com.github.igrishaev;

import java.nio.ByteBuffer;

public class AuthenticationMD5Password {

    public static Integer status = 5;
    public final byte[] salt;

    public AuthenticationMD5Password (ByteBuffer buf) {
        salt = new byte[4];
        buf.get(salt);
    }

}
