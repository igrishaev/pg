package com.github.igrishaev;

import java.nio.ByteBuffer;

public class CopyData extends AMessage {

    public final byte[] bytes;

    public CopyData (byte[] bytes) {
        this.bytes = bytes;
    }

    // TODO: optimize!
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addBytes(bytes)
            .toByteBuffer('d');
    }

}
