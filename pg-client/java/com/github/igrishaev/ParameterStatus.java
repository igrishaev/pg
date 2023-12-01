package com.github.igrishaev;

import java.nio.ByteBuffer;

public record ParameterStatus (String param, String value) {
    public static ParameterStatus fromByteBuffer(ByteBuffer buf) {
        String param = BBUtil.getCString(buf, "UTF-8");
        String value = BBUtil.getCString(buf, "UTF-8");
        return new ParameterStatus(param, value);
    }
}
