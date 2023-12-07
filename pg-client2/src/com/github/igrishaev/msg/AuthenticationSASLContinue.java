package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;

public record AuthenticationSASLContinue (String serverFirstMessage) {
    public static Integer status = 11;
    public static AuthenticationSASLContinue fromByteBuffer(ByteBuffer buf) {
        String message = BBTool.getRestString(buf);
        return new AuthenticationSASLContinue(message);
    }
}
