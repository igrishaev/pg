package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.List;

public class Parse extends AMessage {

    public final String statement;
    public final String query;
    public final List<Short> oids;

    public Parse (String statement, String query, List<Short> oids) {
        this.statement = statement;
        this.query = query;
        this.oids = oids;
    }

    public ByteBuffer encode(String encoding) {

        Payload payload = new Payload();

        payload
            .addCString(statement, encoding)
            .addCString(query, encoding)
            .addShort((short) oids.size());

        for(Short oid: oids) {
            payload.addShort(oid);
        }

        return payload.toByteBuffer('P');
    }

}
