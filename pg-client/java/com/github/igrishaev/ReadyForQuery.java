package com.github.igrishaev;

import java.nio.ByteBuffer;

public record ReadyForQuery (byte txStatus) {
    public ReadyForQuery(ByteBuffer buf) {
        this(buf.get());
    }
}
