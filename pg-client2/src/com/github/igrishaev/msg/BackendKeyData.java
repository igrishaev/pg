package com.github.igrishaev.msg;

import java.nio.ByteBuffer;

public record BackendKeyData (int pid, int secretKey) {
    public static BackendKeyData fromByteBuffer(ByteBuffer buf) {
        int pid = buf.getInt();
        int key = buf.getInt();
        return new BackendKeyData(pid, key);
    }
}
