package com.github.igrishaev;

import java.nio.ByteBuffer;

public abstract class AMessage {

    public ByteBuffer encode(String encoding) {
        throw new PGError("encode method is not implemented");
    }

}
