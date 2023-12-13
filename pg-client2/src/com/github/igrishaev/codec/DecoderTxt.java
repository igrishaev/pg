package com.github.igrishaev.codec;

import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.JSON;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.math.BigDecimal;

public class DecoderTxt extends ACodec {

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
            case JSON, JSONB -> JSON.readValue(buf);
            case TIMESTAMPTZ -> DateTimeTxt.decodeTIMESTAMPTZ(getString(buf));
            case TIMESTAMP -> DateTimeTxt.decodeTIMESTAMP(getString(buf));
            case DATE -> DateTimeTxt.decodeDATE(getString(buf));
            default -> getString(buf);
        };
    }

}
