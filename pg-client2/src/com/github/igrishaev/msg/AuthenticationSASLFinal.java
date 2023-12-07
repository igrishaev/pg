package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;

public record AuthenticationSASLFinal (String serverFinalMessage) {
    public static Integer status = 12;
    public static AuthenticationSASLFinal fromByteBuffer(ByteBuffer buf) {
        String message = BBTool.getRestString(buf);
        return new AuthenticationSASLFinal(message);
    }
}
