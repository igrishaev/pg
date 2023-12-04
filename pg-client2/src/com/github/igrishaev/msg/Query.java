package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import com.github.igrishaev.msg.IMessage;

import java.nio.ByteBuffer;

public record Query (String query) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(query, encoding)
            .toByteBuffer('Q');
    }
}
