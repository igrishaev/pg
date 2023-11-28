package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

public class Parse extends AMessage {

    public final String statement;
    public final String query;
    public final Integer oidCount;
    public final List<Long> oids;

    public Parse (String statement, String query, List<Long> oids) {

        this.statement = statement;
        this.query = query;

        if (oids == null) {
            this.oidCount = 0;
            this.oids = new ArrayList<Long>();
        }
        else {
            this.oids = oids;
            this.oidCount = oids.size();
        }

    }

    public ByteBuffer encode(String encoding) {

        if (oidCount > 0xFFFF) {
            throw new PGError("Too many OIDs! OID count: %s, query: %s",
                              oidCount,
                              query);
        }

        Payload payload = new Payload();

        payload
            .addCString(statement, encoding)
            .addCString(query, encoding)
            .addUnsignedShort(oidCount);

        // TODO: fix oid types
        for(Long oid: oids) {
            payload.addInteger(oid.intValue());
        }

        return payload.toByteBuffer('P');
    }

}
