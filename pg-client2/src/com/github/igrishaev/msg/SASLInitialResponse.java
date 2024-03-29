package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import com.github.igrishaev.enums.SASL;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record SASLInitialResponse(
        SASL saslType,
        String clientFirstMessage
) implements IMessage {

    public ByteBuffer encode(final Charset charset) {
        final Payload payload = new Payload().addCString(saslType().toCode());
        if (clientFirstMessage.isEmpty()) {
            payload.addInteger(-1);
        }
        else {
            final byte[] bytes = clientFirstMessage.getBytes(charset);
            payload.addInteger(bytes.length);
            payload.addBytes(bytes);
        }
        return payload.toByteBuffer('p');
    }

}
