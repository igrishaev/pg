package com.github.igrishaev.msg;

import com.github.igrishaev.enums.OID;
import com.github.igrishaev.PGError;
import com.github.igrishaev.Payload;

import java.nio.ByteBuffer;

public record Parse (String statement,
                     String query,
                     OID[] OIDs)
        implements IMessage {

    public ByteBuffer encode(String encoding) {

        int OIDCount = OIDs.length;

        if (OIDCount > 0xFFFF) {
            throw new PGError(
                    "Too many OIDs! OID count: %s, query: %s",
                    OIDCount, query
            );
        }

        Payload payload = new Payload();

        payload
            .addCString(statement, encoding)
            .addCString(query, encoding)
            .addUnsignedShort(OIDCount);

        for (OID oid: OIDs) {
            payload.addInteger(oid.toInt());
        }

        return payload.toByteBuffer('P');
    }

}
