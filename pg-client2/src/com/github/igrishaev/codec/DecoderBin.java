
package com.github.igrishaev.codec;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.BBTool;
import com.github.igrishaev.util.JSON;


public class DecoderBin {

    public static Object decode(final ByteBuffer buf, final OID oid) {
        return decode(buf, oid, CodecParams.standard());
    }

    public static Object decode(
            final ByteBuffer buf,
            final OID oid,
            final CodecParams codecParams
    ) {
        return switch (oid) {
            case TEXT, VARCHAR, NAME -> BBTool.getRestString(buf, codecParams.serverCharset);
            case INT2 -> buf.getShort();
            case INT4, OID -> buf.getInt();
            case INT8 -> buf.getLong();
            case CHAR -> (char) buf.get();
            case UUID -> {
                long hiBits = buf.getLong();
                long loBits = buf.getLong();
                yield new UUID(hiBits, loBits);
            }
            case FLOAT4 -> buf.getFloat();
            case FLOAT8 -> buf.getDouble();
            case BOOL -> {
                switch (buf.get()) {
                    case 0: yield false;
                    case 1: yield true;
                    default: throw new PGError("incorrect binary boolean value");
                }
            }
            case JSON, JSONB -> JSON.readValueBinary(buf);
            case TIME -> DateTimeBin.decodeTIME(buf);
            case TIMETZ -> DateTimeBin.decodeTIMETZ(buf);
            case DATE -> DateTimeBin.decodeDATE(buf);
            case TIMESTAMP -> DateTimeBin.decodeTIMESTAMP(buf);
            case TIMESTAMPTZ -> DateTimeBin.decodeTIMESTAMPTZ(buf);
            case NUMERIC -> NumericBin.decode(buf);
            default -> BBTool.getRestBytes(buf);
        };
    }

}