package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.Map;

public class StartupMessage extends AMessage {

    public final Integer protocolVersion;
    public final String user;
    public final String database;
    public final Map<String, String> options;

    public StartupMessage (Integer protocolVersion,
                           String user,
                           String database,
                           Map<String, String> options) {

        this.protocolVersion = protocolVersion;
        this.user = user;
        this.database = database;
        this.options = options;
    }

    public ByteBuffer encode(String encoding) {

        Payload payload = new Payload();

        payload
            .addInteger(protocolVersion)
            .addCString("user")
            .addCString(user, encoding)
            .addCString("database")
            .addCString(database, encoding);

        if (options != null) {
            for (Map.Entry<String, String> entry: options.entrySet()) {
                payload.addCString(entry.getKey(), encoding);
                payload.addCString(entry.getValue(), encoding);
            }
        }

        payload.addByte((byte)0);

        return payload.toByteBuffer(null);
    }

}
