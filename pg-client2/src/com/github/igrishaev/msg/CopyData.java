package com.github.igrishaev.msg;

import java.nio.ByteBuffer;

public record CopyData (byte[] bytes, int off, int len) implements IMessage {

    public CopyData(final byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public ByteBuffer encode(final String encoding) {
        final ByteBuffer buf = ByteBuffer.allocate(1 + 4 + len);
        buf.put((byte)'d');
        buf.putInt(4 + len);
        buf.put(bytes, off, len);
        return buf;
    }

    public static CopyData fromByteBuffer(final ByteBuffer buf) {
        final byte[] bytes = buf.array();
        final int off = buf.arrayOffset() + buf.position();
        final int len = buf.limit();
        return new CopyData(bytes, off, len);
    }
}
