package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.Map;

public class Query extends AMessage {

    public final String query;

    public Query (String query) {
        this.query = query;
    }

    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(query, encoding)
            .toByteBuffer('Q');
    }

}
