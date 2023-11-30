package com.github.igrishaev;

import java.nio.ByteBuffer;

public record SSLRequest (int sslCode) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return ByteBuffer.allocate(8).putInt(8).putInt(sslCode);
    }
}
