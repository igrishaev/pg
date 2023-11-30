package com.github.igrishaev;

import java.nio.ByteBuffer;

public record CopyData (byte[] bytes) implements IMessage {
    // TODO: optimize!
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addBytes(bytes)
            .toByteBuffer('d');
    }
}
