package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import com.github.igrishaev.msg.IMessage;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Query (String query) implements IMessage {
    public ByteBuffer encode(final Charset charset) {
        return new Payload()
            .addCString(query, charset)
            .toByteBuffer('Q');
    }
}
