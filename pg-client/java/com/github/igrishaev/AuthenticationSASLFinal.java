package com.github.igrishaev;

import java.nio.ByteBuffer;

public class AuthenticationSASLFinal {

    public static Integer status = 12;
    public final String serverFinalMessage;

    public AuthenticationSASLFinal(ByteBuffer buf) {
        serverFinalMessage = BBUtil.getRestString(buf);
    }

}
