package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record AuthenticationSASLFinal (String serverFinalMessage) {
    public static Integer status = 12;
    public static AuthenticationSASLFinal fromByteBuffer(
            final ByteBuffer buf,
            final Charset charset
    ) {
        String message = BBTool.getRestString(buf, charset);
        return new AuthenticationSASLFinal(message);
    }
}
