package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;


public record RowDescription (
        short columnCount,
        ArrayList<Column> columns) {

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

    public RowDescription (ByteBuffer buf) {
        this(
                buf.getShort(),
                new ArrayList<>()
        );

        for (int i = 0; i < columnCount; i++) {
            Column col = new Column(i,
                                    BBUtil.getCString(buf, "UTF-8"),
                                    buf.getInt(),
                                    buf.getShort(),
                                    buf.getInt(),
                                    buf.getShort(),
                                    buf.getInt(),
                                    buf.getShort());
            columns.add(col);
        }

    }

}
