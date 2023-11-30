package com.github.igrishaev;

import java.nio.ByteBuffer;

public record ParameterStatus (String param, String value) {
    public ParameterStatus(ByteBuffer buf) {
        this(
                BBUtil.getCString(buf, "UTF-8"),
                BBUtil.getCString(buf, "UTF-8")
        );
    }
}
