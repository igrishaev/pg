package com.github.igrishaev;

import java.nio.ByteBuffer;

public class AuthenticationResponse {

    public final Integer status;

    public AuthenticationResponse(ByteBuffer buf) {

        status = buf.getInt();

    }

}
