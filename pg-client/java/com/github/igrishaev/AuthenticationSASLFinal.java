package com.github.igrishaev;

import java.nio.ByteBuffer;

public record AuthenticationSASLFinal (String serverFinalMessage) {
    public static Integer status = 12;
    public AuthenticationSASLFinal(ByteBuffer buf) {
        this(BBUtil.getRestString(buf));
    }
}
