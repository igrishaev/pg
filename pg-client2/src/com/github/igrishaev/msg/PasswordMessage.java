package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;

import java.nio.ByteBuffer;

public record PasswordMessage (String password) implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(password, encoding)
            .toByteBuffer('p');
    }
}
