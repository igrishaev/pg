package com.github.igrishaev;

import java.nio.ByteBuffer;

public record Flush () implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('H');
    }

}
