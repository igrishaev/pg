package com.github.igrishaev;

import java.nio.ByteBuffer;

public class CopyDone extends AMessage {

    // TODO: optimize!
    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('c');
    }

}
