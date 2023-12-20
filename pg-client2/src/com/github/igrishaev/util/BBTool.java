package com.github.igrishaev.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class BBTool {

    public static String getCString (final ByteBuffer buf, final Charset charset) {
        final int pos = buf.position();
        int len = 0;
        while (buf.get(pos + len) != 0) {
            len++;
        }
        skip(buf, len + 1);
        return new String(buf.array(), pos, len, charset);
    }

    public static Boolean isEnd (final ByteBuffer buf) {
        return buf.remaining() == 0;
    }

    public static String getRestString (final ByteBuffer buf, Charset charset) {
        return new String(
                buf.array(),
                buf.arrayOffset() + buf.position(),
                buf.remaining(),
                charset
        );
    }

    public static byte[] getRestBytes (final ByteBuffer buf) {
        final int size = buf.limit();
        final byte[] bytes = new byte[size];
        buf.get(bytes);
        return bytes;
    }

    public static String getString(final ByteBuffer buf, final Charset charset) {
        final int offset = buf.arrayOffset() + buf.position();
        return new String(buf.array(), offset, buf.limit(), charset);
    }

    public static void skip (final ByteBuffer buf, final int offset) {
        buf.position(buf.position() + offset);
    }

}
