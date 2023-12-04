package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;

import java.nio.ByteBuffer;

public record CopyData (byte[] bytes) implements IMessage {
    // TODO: optimize!
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addBytes(bytes)
            .toByteBuffer('d');
    }
}
