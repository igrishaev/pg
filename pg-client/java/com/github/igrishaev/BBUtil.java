package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;


public class BBUtil {

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
            throw new PGError(e, "cannot decode a C-string");
        }

    }

    public static Boolean isEnd (ByteBuffer buf) {
        return buf.remaining() == 0;
    }

    public static String getRestString (ByteBuffer buf) {
        try {
            return new String(buf.array(),
                              buf.position(),
                              buf.remaining(),
                              "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new PGError(e, "cannot get the rest string");
        }
    }

    public static void skip (ByteBuffer buf, int offset) {
        buf.position(buf.position() + offset);
    }

    public static ByteBuffer slice (ByteBuffer buf, int offset) {
        skip(buf, offset);
        return buf.slice(buf.position(), offset);
    }

}
