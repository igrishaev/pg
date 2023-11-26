package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;


public class BBUtil {

    public static String getCString (ByteBuffer buf) {
        return getCString(buf, "UTF-8");
    }

    public static String getCString (ByteBuffer buf, String encoding) {

        int pos = buf.position();
        int len = 0;

        while (buf.get(pos + len) != 0) {
            len++;
        }

        skip(buf, len + 1);

        try {
            return new String(buf.array(), pos, len, encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new PGError("Cannot read string, pos: %s, len: %s", pos, len);
        }

    }

    public static void skip (ByteBuffer buf, int offset) {
        buf.position(buf.position() + offset);
    }

    public static ByteBuffer slice (ByteBuffer buf, int offset) {
        return buf.slice(buf.position(), offset);

    }

}
