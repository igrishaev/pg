package com.github.igrishaev;

import java.nio.ByteBuffer;

public class SSLRequest extends AMessage {

    public final Integer sslCode;

    public SSLRequest (Integer sslCode) {
        this.sslCode = sslCode;
    }

    public ByteBuffer encode(String encoding) {
        return ByteBuffer.allocate(8).putInt(8).putInt(sslCode);
    }

}
