package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import com.github.igrishaev.enums.SourceType;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Close(SourceType sourceType, String source) implements IMessage {
    public ByteBuffer encode(final Charset charset) {
        return new Payload()
                .addByte((byte)sourceType.getCode())
                .addCString(source, charset)
                .toByteBuffer('C');
    }
}
