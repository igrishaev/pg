package com.github.igrishaev.util;

import com.github.igrishaev.PGError;

import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;


public class BBTool {

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
        return new String(buf.array(),
                          buf.arrayOffset() + buf.position(),
                          buf.remaining(),
                StandardCharsets.UTF_8); // TODO: encoding
    }

    public static byte[] getRestBytes (ByteBuffer buf) {
        int size = buf.limit();
        byte[] bytes = new byte[size];
        buf.get(bytes);
        return bytes;
    }

    public static String getString(ByteBuffer buf, String encoding) {
        int offset = buf.arrayOffset() + buf.position();
        try {
            return new String(buf.array(), offset, buf.limit(), encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new PGError(e, "could not get a string");
        }
    }

    public static void skip (ByteBuffer buf, int offset) {
        buf.position(buf.position() + offset);
    }

}
