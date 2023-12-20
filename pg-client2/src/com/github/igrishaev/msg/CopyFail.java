package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record CopyFail (String errorMessage) implements IMessage {
    public ByteBuffer encode(final Charset charset) {
        return new Payload()
            .addCString(errorMessage, charset)
            .toByteBuffer('f');
    }
}
