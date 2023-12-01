package com.github.igrishaev;

import java.nio.ByteBuffer;

public record AuthenticationSASLFinal (String serverFinalMessage) {
    public static Integer status = 12;
    public static AuthenticationSASLFinal fromByteBuffer(ByteBuffer buf) {
        String message = BBUtil.getRestString(buf);
        return new AuthenticationSASLFinal(message);
    }
}
