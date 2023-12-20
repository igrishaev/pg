package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import com.github.igrishaev.msg.IMessage;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

public record StartupMessage (Integer protocolVersion,
                              String user,
                              String database,
                              Map<String, String> options
) implements IMessage {
    public ByteBuffer encode(final Charset charset) {
        final Payload payload = new Payload();
        payload
            .addInteger(protocolVersion)
            .addCString("user")
            .addCString(user, charset)
            .addCString("database")
            .addCString(database, charset);
        for (Map.Entry<String, String> entry: options.entrySet()) {
            payload.addCString(entry.getKey(), charset);
            payload.addCString(entry.getValue(), charset);
        }
        payload.addByte((byte)0);
        return payload.toByteBuffer();
    }
}
