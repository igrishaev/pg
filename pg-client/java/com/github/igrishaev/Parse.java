package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.List;

public record Parse (String statement,
                     String query,
                     List<Long> OIDs)
        implements IMessage {

    public int getOIDCount() {
        return (OIDs == null) ? 0 : OIDs.size();
    }
    public ByteBuffer encode(String encoding) {
        int OIDCount = getOIDCount();

        if (OIDCount > 0xFFFF) {
            throw new PGError("Too many OIDs! OID count: %s, query: %s",
                    OIDCount,
                    query);
        }

        Payload payload = new Payload();

        payload
            .addCString(statement, encoding)
            .addCString(query, encoding)
            .addUnsignedShort(OIDCount);

        // TODO: fix oid types
        if (OIDs != null) {
            for(Long oid: OIDs) {
               payload.addInteger(oid.intValue());
            }
        }

        return payload.toByteBuffer('P');
    }

}
