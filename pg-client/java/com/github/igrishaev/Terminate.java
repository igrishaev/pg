package com.github.igrishaev;

import java.nio.ByteBuffer;

public class Terminate extends AMessage {

    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('X');
    }

}
