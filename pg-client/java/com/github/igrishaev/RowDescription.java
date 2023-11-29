package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RowDescription  {

    public class Column {

        final public int    index;
        final public String name;
        final public int    tableOid;
        final public int    columnOid;
        final public int    typeOid;
        final public short  typeLen;
        final public int    typeMod;
        final public short  format;

        public Column (int    index,
                       String name,
                       int    tableOid,
                       int    columnOid,
                       int    typeOid,
                       short  typeLen,
                       int    typeMod,
                       short  format) {

            this.index     = index;
            this.name      = name;
            this.tableOid  = tableOid;
            this.columnOid = columnOid;
            this.typeOid   = typeOid;
            this.typeLen   = typeLen;
            this.typeMod   = typeMod;
            this.format    = format;

        }

    }

    public final short columnCount;
    public final ArrayList<Column> columns;

    public RowDescription (ByteBuffer buf) {

        columnCount = buf.getShort();
        columns = new ArrayList<Column>();

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
