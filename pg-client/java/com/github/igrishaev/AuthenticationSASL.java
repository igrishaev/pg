package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.HashSet;

public record AuthenticationSASL (HashSet<String> SASLTypes) {
    public static Integer status = 10;
    public AuthenticationSASL(ByteBuffer buf) {
        this(new HashSet<>());
        while (!BBUtil.isEnd(buf)) {
            String type = BBUtil.getCString(buf, "UTF-8"); // TODO:
            SASLTypes.add(type);
        }
    }
}
