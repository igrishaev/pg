package com.github.igrishaev;

import java.nio.ByteBuffer;

public record CopyFail (String errorMessage) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(errorMessage, encoding)
            .toByteBuffer('f');
    }
}
