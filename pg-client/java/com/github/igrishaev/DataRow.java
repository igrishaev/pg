package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DataRow {

    public final short valueCount;
    public final ArrayList<ByteBuffer> values;

    public DataRow (ByteBuffer buf) {

        valueCount = buf.getShort();
        values = new ArrayList<ByteBuffer>();

        for (int i = 0; i < valueCount; i++) {

            int len = buf.getInt();

            if (len == -1) {
                values.add(null);
            }
            else {
                ByteBuffer bufValue = buf.slice();
                bufValue.limit(len);
                BBUtil.skip(buf, len);
                values.add(bufValue);
            }

        }

    }

}
