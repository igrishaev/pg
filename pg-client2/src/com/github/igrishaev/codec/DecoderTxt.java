package com.github.igrishaev.codec;

import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.BBTool;
import com.github.igrishaev.util.JSON;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.math.BigDecimal;

public class DecoderTxt {

    private static String getString(ByteBuffer buf, CodecParams codecParams) {
        return BBTool.getCString(buf, codecParams.serverEncoding);
    }

    public static Object decode(ByteBuffer buf, OID oid) {
        return decode(buf, oid, CodecParams.standard());
    }

    public static Object decode(ByteBuffer buf, OID oid, CodecParams codecParams) {

        return switch (oid) {

            case INT2 -> Short.parseShort(getString(buf, codecParams));
            case INT4, OID -> Integer.parseInt(getString(buf, codecParams));
            case INT8 -> Long.parseLong(getString(buf, codecParams));
            case BYTEA -> buf.array(); // TODO: decode!
            case CHAR -> buf.getChar(); // TODO: test
            case UUID -> UUID.fromString(getString(buf, codecParams));
            case FLOAT4 -> Float.parseFloat(getString(buf, codecParams));
            case FLOAT8 -> Double.parseDouble(getString(buf, codecParams));
            case NUMERIC -> new BigDecimal(getString(buf, codecParams));
            case BOOL -> {
                byte b = buf.get();
                yield switch ((char) b) {
                    case 't' -> true;
                    case 'f' -> false;
                    default -> throw new PGError("wrong boolean value: %s", b);
                };
            }
            case JSON, JSONB -> JSON.readValue(buf);
            case TIMESTAMPTZ -> DateTimeTxt.decodeTIMESTAMPTZ(getString(buf, codecParams));
            case TIMESTAMP -> DateTimeTxt.decodeTIMESTAMP(getString(buf, codecParams));
            case DATE -> DateTimeTxt.decodeDATE(getString(buf, codecParams));
            case TIMETZ -> DateTimeTxt.decodeTIMETZ(getString(buf, codecParams));
            case TIME -> DateTimeTxt.decodeTIME(getString(buf, codecParams));
            default -> getString(buf, codecParams);
        };
    }

}
