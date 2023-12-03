package com.github.igrishaev.msg;

import com.github.igrishaev.enums.OID;
import com.github.igrishaev.PGError;
import com.github.igrishaev.Payload;

import java.nio.ByteBuffer;
import java.util.List;

public record Parse (String statement,
                     String query,
                     List<OID> OIDs)
        implements IMessage {

    public ByteBuffer encode(String encoding) {

        int OIDCount = OIDs.size();

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

        return payload.toByteBuffer('P');
    }

}
