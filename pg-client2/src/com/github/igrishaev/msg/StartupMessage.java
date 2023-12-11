package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import com.github.igrishaev.msg.IMessage;

import java.nio.ByteBuffer;
import java.util.Map;

public record StartupMessage (Integer protocolVersion,
                              String user,
                              String database,
                              Map<String, String> options
) implements IMessage {
    public ByteBuffer encode(String encoding) {
        Payload payload = new Payload();
        payload
            .addInteger(protocolVersion)
            .addCString("user")
            .addCString(user, encoding)
            .addCString("database")
            .addCString(database, encoding);
        for (Map.Entry<String, String> entry: options.entrySet()) {
            payload.addCString(entry.getKey(), encoding);
            payload.addCString(entry.getValue(), encoding);
        }
        payload.addByte((byte)0);
        return payload.toByteBuffer();
    }
}
