package com.github.igrishaev;

import java.nio.ByteBuffer;

public record Execute (String portal, Long rowCount) implements IMessage {
    public Execute (String portal, Long rowCount) {
        this.portal = portal;
        this.rowCount = rowCount;
        if (rowCount > 0xFFFFFFFFL) {
            throw new PGError("Too many rows: %s", rowCount);
        }
    }
    public ByteBuffer encode(String encoding) {
        return new Payload()
            .addCString(portal, encoding)
            .addUnsignedInteger(rowCount)
            .toByteBuffer('E');
    }
}