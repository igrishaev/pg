package com.github.igrishaev.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.github.igrishaev.enums.Format;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.Payload;

public record Bind (
        String portal,
        String statement,
        byte[][] values,
        OID[] OIDs,
        Format paramsFormat,
        Format columnFormat
) implements IMessage {

    public ByteBuffer encode(final Charset charset) {
        final Payload payload = new Payload()
                .addCString(portal)
                .addCString(statement)
                .addShort((short)1)
                .addShort(paramsFormat.toCode())
                .addUnsignedShort(values.length);

        for (byte[] bytes: values) {
            if (bytes == null) {
                payload.addInteger(-1);
            }
            else {
                payload.addInteger(bytes.length);
                payload.addBytes(bytes);
            }
        }

        payload.addShort((short)1);
        payload.addShort(columnFormat.toCode());

        return payload.toByteBuffer('B');
    }
}
