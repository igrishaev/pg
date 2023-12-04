package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;

import java.nio.ByteBuffer;

public record CopyFail (String errorMessage) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(errorMessage, encoding)
            .toByteBuffer('f');
    }
}
