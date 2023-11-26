package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DataRow {

    private final short valueCount;
    private ArrayList<ByteBuffer> values;

    public ArrayList getValues () {
        return values;
    }

    public short valueCount() {
        return valueCount;
    }

    public DataRow (ByteBuffer buf) {

        this.valueCount = buf.getShort();

        for (int i = 0; i < valueCount ; i++) {
            int len = buf.getInt();

            if (len == -1) {
                values.add(null);
            }
            else {
                ByteBuffer value = BBUtil.slice(buf, len);
                BBUtil.skip(buf, len);
                values.add(value);
            }

        }

    }

}
