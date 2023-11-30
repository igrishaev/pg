package com.github.igrishaev;

import java.nio.ByteBuffer;

public record Sync () implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('S');
    }
}
