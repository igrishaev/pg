package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public record AuthenticationSASL (HashSet<String> SASLTypes) {
    public static Integer status = 10;
    public static AuthenticationSASL fromByteBuffer(ByteBuffer buf) {
        HashSet<String> types = new HashSet<>();
        while (!BBUtil.isEnd(buf)) {
            String type = BBUtil.getCString(buf, "UTF-8"); // TODO:
            types.add(type);
        }
        return new AuthenticationSASL(types);
    }
}
