package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public record DataRow (short valueCount, ByteBuffer[] values) {
    public static DataRow fromByteBuffer(ByteBuffer buf) {
        short size = buf.getShort();
        ByteBuffer[] values = new ByteBuffer[size];
        for (int i = 0; i < size; i++) {
            int len = buf.getInt();
            if (len == -1) {
                values[i] = null;
            }
            else {
                ByteBuffer bufValue = buf.slice();
                bufValue.limit(len);
                BBUtil.skip(buf, len);
                values[i] = bufValue;
            }
        };
        return new DataRow(size, values);
    }
}
