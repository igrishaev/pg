package com.github.igrishaev.codec;

import com.github.igrishaev.PGError;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

abstract class ACodec {

    String encoding = "UTF-8";
    String dateStyle = null;
    String timeZone = null;

    public void setEncoding (String encoding) {
        this.encoding = encoding;
    }

    public void setDateStyle (String dateStyle) {
        this.dateStyle = dateStyle;
    }

    public void setTimeZone (String timeZone) {
        this.timeZone = timeZone;
    }

    public String getString(ByteBuffer buf) {
        int offset = buf.arrayOffset() + buf.position();
        try {
            return new String(buf.array(), offset, buf.limit(), encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new PGError(e, "could not get a string");
        }
    }

    public static byte[] getBytes(ByteBuffer buf) {
        int size = buf.limit();
        byte[] bytes = new byte[size];
        buf.get(bytes);
        return bytes;
    }

}
