package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import com.github.igrishaev.enums.SourceType;

import java.nio.ByteBuffer;

public record Describe(SourceType sourceType, String source) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload()
                .addByte((byte)sourceType.getCode())
                .addCString(source, encoding)
                .toByteBuffer('D');
    }
}
