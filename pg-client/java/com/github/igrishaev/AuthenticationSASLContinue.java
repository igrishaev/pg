package com.github.igrishaev;

import java.nio.ByteBuffer;

public record AuthenticationSASLContinue (String serverFirstMessage) {
    public static Integer status = 11;
    public static AuthenticationSASLContinue fromByteBuffer(ByteBuffer buf) {
        String message = BBUtil.getRestString(buf);
        return new AuthenticationSASLContinue(message);
    }
}
