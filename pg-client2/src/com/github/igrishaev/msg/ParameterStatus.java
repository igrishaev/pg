package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record ParameterStatus (String param, String value) {
    public static ParameterStatus fromByteBuffer(final ByteBuffer buf, final Charset charset) {
        String param = BBTool.getCString(buf, charset);
        String value = BBTool.getCString(buf, charset);
        return new ParameterStatus(param, value);
    }
}
