package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.Map;

public record Query (String query) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(query, encoding)
            .toByteBuffer('Q');
    }
}
