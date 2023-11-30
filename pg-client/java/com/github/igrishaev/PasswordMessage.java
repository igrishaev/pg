package com.github.igrishaev;

import java.nio.ByteBuffer;

public record PasswordMessage (String password) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(password, encoding)
            .toByteBuffer('p');
    }
}
