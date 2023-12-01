package com.github.igrishaev;

import java.nio.ByteBuffer;

public record ReadyForQuery (byte txStatus) {
    public static ReadyForQuery fromByteBuffer(ByteBuffer buf) {
        byte status = buf.get();
        return new ReadyForQuery(status);
    }
}
