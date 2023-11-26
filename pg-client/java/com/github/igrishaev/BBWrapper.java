package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;


public class BBWrapper {

    private String serverEncoding = "UTF-8";
    private String clientEncoding = "UTF-8";

    public BBWrapper (String serverEncoding, String clientEncoding) {
        this.serverEncoding = serverEncoding;
        this.clientEncoding = clientEncoding;
    }

    public void setServerEncoding(String encoding) {
        this.serverEncoding = encoding;
    }

    public void setClientEncoding(String encoding) {
        this.clientEncoding = encoding;
    }

    public String getCString (ByteBuffer buf) {

        int pos = buf.position();
        int len = 0;

        while (buf.get(pos + len) != 0) {
            len++;
        }

        skip(buf, len + 1);

        try {
            return new String(buf.array(), pos, len, serverEncoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new Error("aaa");
        }

    }

    public void skip (ByteBuffer buf, int offset) {
        buf.position(buf.position() + offset);
    }

    public ByteBuffer slice (ByteBuffer buf, int offset) {
        skip(buf, offset + 1);
        return buf.slice(buf.position(), offset);
    }

}
