
package com.github.igrishaev.codec;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.JSON;


public class DecoderBin extends ACodec {

    public Object decode(ByteBuffer buf, OID oid) {
        return switch (oid) {
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
            default -> getBytes(buf);
        };
    }

}