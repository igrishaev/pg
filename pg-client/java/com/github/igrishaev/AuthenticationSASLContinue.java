package com.github.igrishaev;

import java.nio.ByteBuffer;

public record AuthenticationSASLContinue (String serverFirstMessage) {
    public static Integer status = 11;
    public AuthenticationSASLContinue(ByteBuffer buf) {
        this(BBUtil.getRestString(buf));
    }

}
