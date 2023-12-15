package com.github.igrishaev.msg;

import com.github.igrishaev.enums.Format;

import java.nio.ByteBuffer;

public record CopyInResponse(Format format,
                             short columnCount,
                             Format[] columnFormats
) {

    public static CopyInResponse fromByteBuffer (ByteBuffer buf) {
        Format format = Format.ofShort(buf.get());
        short columnCount = buf.getShort();
        Format[] columnFormats = new Format[columnCount];
        for (short i = 0; i < columnCount; i++) {
            columnFormats[i] = Format.ofShort(buf.getShort());
        }
        return new CopyInResponse(format, columnCount, columnFormats);
    }

}