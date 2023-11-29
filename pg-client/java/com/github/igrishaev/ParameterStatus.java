package com.github.igrishaev;

import java.nio.ByteBuffer;

public class ParameterStatus {

    public final String param;
    public final String value;

    public ParameterStatus(ByteBuffer buf) {
        param = BBUtil.getCString(buf, "UTF-8");
        value = BBUtil.getCString(buf, "UTF-8");
    }

}
