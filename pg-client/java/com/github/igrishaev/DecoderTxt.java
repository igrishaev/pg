package com.github.igrishaev;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.math.BigDecimal;

public class DecoderTxt {

    private String encoding = Const.UTF8;
    private String dateStyle;
    private String timeZone;

    public void setEncoding (String encoding) {
        this.encoding = encoding;
    }

    public void setDateStyle (String dateStyle) {
        this.dateStyle = dateStyle;
    }

    public void setTimeZone (String timeZone) {
        this.timeZone = timeZone;
    }

    private String getString(ByteBuffer buf) {
        int offset = buf.arrayOffset() + buf.position();
        try {
            return new String(buf.array(), offset, buf.limit(), encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new PGError(e, "could not get a string");
        }
    }

    public Object decode(ByteBuffer buf, OID oid) {

        return switch (oid) {

            case INT2 -> Short.parseShort(getString(buf));
            case INT4, OID -> Integer.parseInt(getString(buf));
            case INT8 -> Long.parseLong(getString(buf));

            case BYTEA -> buf.array();
            case CHAR -> buf.getChar();

            case UUID -> UUID.fromString(getString(buf));

            case FLOAT4 -> Float.parseFloat(getString(buf));
            case FLOAT8 -> Double.parseDouble(getString(buf));

            case NUMERIC -> new BigDecimal(getString(buf));

            case BOOL -> {
                byte b = buf.get();
                yield switch ((char) b) {
                    case 't' -> true;
                    case 'f' -> false;
                    default -> throw new PGError("wrong boolean value: %s", b);
                };
            }
            default -> getString(buf);
        };
    }

}
