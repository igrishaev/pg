package com.github.igrishaev.msg;

import com.github.igrishaev.msg.IMessage;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record SSLRequest (int sslCode) implements IMessage {
    public ByteBuffer encode(final Charset charset) {
        return ByteBuffer.allocate(8).putInt(8).putInt(sslCode);
    }
}
