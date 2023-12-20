package com.github.igrishaev.msg;

import com.github.igrishaev.Const;
import com.github.igrishaev.PGError;
import com.github.igrishaev.Payload;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Execute (String portal, long rowCount) implements IMessage {
    public Execute (final String portal, final long rowCount) {
        this.portal = portal;
        this.rowCount = rowCount;
        if (rowCount > Const.EXE_MAX_ROWS) {
            throw new PGError("Too many rows: %s", rowCount);
        }
    }
    public ByteBuffer encode(final Charset charset) {
        return new Payload()
            .addCString(portal, charset)
            .addUnsignedInteger(rowCount)
            .toByteBuffer('E');
    }
}
