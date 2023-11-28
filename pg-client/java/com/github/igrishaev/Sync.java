package com.github.igrishaev;

import java.nio.ByteBuffer;

public class Sync extends AMessage {

    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('S');
    }

}
