package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;

import java.nio.ByteBuffer;

public record CopyDone () implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('c');
    }
}
