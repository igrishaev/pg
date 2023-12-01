package com.github.igrishaev;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class DecoderTxt {
    private String encoding = "UTF-8";
    public void setEncoding (String encoding) {
        this.encoding = encoding;
    }

    private String getString(ByteBuffer buf) {
        try {
            return new String(buf.array(), encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new PGError(e, "could not get a string");
        }
    }

    public Object decode(ByteBuffer buf, OID oid) {

        String payload = getString(buf);

        return switch (oid) {
            case INT2 -> Short.parseShort(payload);
            case INT4 -> Integer.parseInt(payload);
            case INT8 -> Long.parseLong(payload);
            case BOOL -> {
                byte b = buf.get();
                yield switch ((char) b) {
                    case 't' -> true;
                    case 'f' -> false;
                    default -> throw new PGError("aaa");
                };
            }
            default -> throw new PGError("cannot decode");
        };
    }

}
