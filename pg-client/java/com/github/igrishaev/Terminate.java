package com.github.igrishaev;

import java.nio.ByteBuffer;

public record Terminate () implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('X');
    }
}
