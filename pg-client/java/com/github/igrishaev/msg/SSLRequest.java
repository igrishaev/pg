package com.github.igrishaev.msg;

import com.github.igrishaev.msg.IMessage;

import java.nio.ByteBuffer;

public record SSLRequest (int sslCode) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return ByteBuffer.allocate(8).putInt(8).putInt(sslCode);
    }
}
