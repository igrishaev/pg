package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.HashSet;

public class AuthenticationSASLContinue {

    public static Integer status = 11;
    public String serverFirstMessage;

    public AuthenticationSASLContinue(ByteBuffer buf) {
        serverFirstMessage = BBUtil.getRestString(buf);
    }

}
