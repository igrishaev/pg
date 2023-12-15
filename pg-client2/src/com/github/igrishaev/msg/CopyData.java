package com.github.igrishaev.msg;

import java.nio.ByteBuffer;

public record CopyData (byte[] bytes, int size) implements IMessage {
    // TODO: do not wrap into a BB
    // TODO: do not copy the array!!!

    public ByteBuffer encode(final String encoding) {
        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + size);
        buf.put((byte)'d');
        buf.putInt(4 + size);
        buf.put(bytes, 0, size);
        return buf;
    }

    public static CopyData fromByteBuffer(final ByteBuffer buf) {
        byte[] bytes = buf.array();
        return new CopyData(bytes, bytes.length);
    }
}
