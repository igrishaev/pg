package com.github.igrishaev;

import java.nio.ByteBuffer;

public class CopyFail extends AMessage {

    public final String errorMessage;

    public CopyFail (String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(errorMessage, encoding)
            .toByteBuffer('f');
    }

}
