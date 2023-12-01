package com.github.igrishaev;

import java.nio.ByteBuffer;

public record BackendKeyData (int pid, int secretKey) {
    public BackendKeyData (ByteBuffer buf) {
        this(buf.getInt(), buf.getInt());
    }
}
