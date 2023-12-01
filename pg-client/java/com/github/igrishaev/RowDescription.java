package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.text.Normalizer;

public record RowDescription (
        short columnCount,
        Column[] columns
) {

    public record Column (
            int index,
            String name,
            int tableOid,
            int columnOid,
            OID typeOid,
            short typeLen,
            int typeMod,
            Format format) {
    }

    public static RowDescription fromByteBuffer(ByteBuffer buf) {
        short size = buf.getShort();
        Column[] columns = new Column[size];
        for (short i = 0; i < size; i++) {
            Column col = new Column(i,
                    BBUtil.getCString(buf, "UTF-8"),
                    buf.getInt(),
                    buf.getShort(),
                    OID.ofInt(buf.getInt()),
                    buf.getShort(),
                    buf.getInt(),
                    Format.ofShort(buf.getShort()));
            columns[i] = col;
        }
        return new RowDescription(size, columns);
    }
}
