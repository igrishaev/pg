package com.github.igrishaev;

import java.nio.ByteBuffer;

public record CopyDone () implements IMessage {
    // TODO: optimize!
    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('c');
    }
}
