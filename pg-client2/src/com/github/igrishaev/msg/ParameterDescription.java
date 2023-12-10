package com.github.igrishaev.msg;

import com.github.igrishaev.enums.OID;

import java.nio.ByteBuffer;

public record ParameterDescription (
        int paramCount,
        OID[] OIDs
) {

    public static ParameterDescription fromByteBuffer(ByteBuffer buf) {
        int count = Short.toUnsignedInt(buf.getShort());
        OID[] OIDs = new OID[count];
        for (int i = 0; i < count; i++) {
            OIDs[i] = OID.ofInt(buf.getInt());
        }
        return new ParameterDescription(count, OIDs);
    }
}
