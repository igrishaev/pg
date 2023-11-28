package com.github.igrishaev;

import java.nio.ByteBuffer;

public class PasswordMessage extends AMessage {

    public final String password;

    public PasswordMessage (String password) {
        this.password = password;
    }

    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(password, encoding)
            .toByteBuffer('p');
    }

}
