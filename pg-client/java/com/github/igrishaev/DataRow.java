package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record DataRow (short valueCount, ByteBuffer[] values) {
    public static DataRow fromByteBuffer(ByteBuffer buf) {

        // System.out.println(Arrays.toString(buf.array()));

        short size = buf.getShort();
        ByteBuffer[] values = new ByteBuffer[size];
        for (short i = 0; i < size; i++) {
            int len = buf.getInt();
            if (len == -1) {
                values[i] = null;
            }
            else {
                ByteBuffer bufValue = buf.slice();
                bufValue.limit(len);
                BBUtil.skip(buf, len);
                values[i] = bufValue;

                // System.out.println(Arrays.toString(bufValue.array()));
            }
        }
        return new DataRow(size, values);
    }
}
