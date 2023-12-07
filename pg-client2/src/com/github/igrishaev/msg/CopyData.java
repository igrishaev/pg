package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import java.nio.ByteBuffer;

public record CopyData (byte[] bytes) implements IMessage {
    // TODO: do not wrap into a BB
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addBytes(bytes)
            .toByteBuffer('d');
    }

    public static CopyData fromByteBuffer(ByteBuffer buf) {
        return new CopyData(buf.array());
    }
}
