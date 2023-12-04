package com.github.igrishaev.msg;

import com.github.igrishaev.BBUtil;
import com.github.igrishaev.enums.Format;
import com.github.igrishaev.enums.OID;

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
