package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.HashSet;

public class AuthenticationSASL {

    public static Integer status = 10;
    public final HashSet<String> SASLtypes;

    public AuthenticationSASL(ByteBuffer buf) {

        SASLtypes = new HashSet<String>();

    }

}
