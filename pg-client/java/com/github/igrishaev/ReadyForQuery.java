package com.github.igrishaev;

import java.nio.ByteBuffer;

public record ReadyForQuery (TXStatus txStatus) {
    public static ReadyForQuery fromByteBuffer(ByteBuffer buf) {
        TXStatus status = TXStatus.ofChar((char) buf.get());
        return new ReadyForQuery(status);
    }
}
