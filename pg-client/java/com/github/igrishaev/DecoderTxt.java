package com.github.igrishaev;

import java.nio.ByteBuffer;

public class DecoderTxt {

    public String encoding = "UTF-8";

    public Object decode(ByteBuffer buf, int oid) {
        return switch (oid) {
            case 20 -> buf.getLong();
            case 23 -> buf.getInt();
            case 21 -> buf.getShort();
            default -> buf;
        };
    }

}
