package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record AuthenticationSASLContinue (String serverFirstMessage) {
    public static Integer status = 11;
    public static AuthenticationSASLContinue fromByteBuffer(
            final ByteBuffer buf,
            final Charset charset) {
        String message = BBTool.getRestString(buf, charset);
        return new AuthenticationSASLContinue(message);
    }
}
