package com.github.igrishaev.codec;

import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;

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
    public byte[] getBytes (String string) {
        try {
            return string.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new PGError(e, "could not get bytes from a string");
        }
    }

    public static String txtEncodingError(Object x, OID oid) {
        throw new PGError("cannot text-encode a value: %s, OID: %s", x, oid);
    }

    public static ByteBuffer binEncodingError(Object x, OID oid) {
        throw new PGError("cannot binary-encode a value: %s, OID: %s", x, oid);
    }

}
