package com.github.igrishaev;

import java.nio.ByteBuffer;

public record RowDescription (
        short columnCount,
        Column[] columns
) {

    public record Column (
            int index,
            String name,
            int tableOid,
            int columnOid,
            int typeOid,
            short typeLen,
            int typeMod,
            short format) {
    }

    public static RowDescription fromByteBuffer(ByteBuffer buf) {
        short size = buf.getShort();
        Column[] columns = new Column[size];
        for (short i = 0; i < size; i++) {
            Column col = new Column(i,
                    BBUtil.getCString(buf, "UTF-8"),
                    buf.getInt(),
                    buf.getShort(),
                    buf.getInt(),
                    buf.getShort(),
                    buf.getInt(),
                    buf.getShort());
            columns[i] = col;
        }
        return new RowDescription(size, columns);
    }
}
