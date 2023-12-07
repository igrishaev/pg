package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;

public record ParameterStatus (String param, String value) {
    public static ParameterStatus fromByteBuffer(ByteBuffer buf) {
        String param = BBTool.getCString(buf, "UTF-8");
        String value = BBTool.getCString(buf, "UTF-8");
        return new ParameterStatus(param, value);
    }
}
