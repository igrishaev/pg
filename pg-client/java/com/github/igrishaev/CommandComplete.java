package com.github.igrishaev;

import java.nio.ByteBuffer;

public class CommandComplete {

    public final String tag;

    public CommandComplete (ByteBuffer buf) {
        this(buf, "UTF-8");
    }

    public CommandComplete (ByteBuffer buf, String encoding) {
        tag = BBUtil.getCString(buf, encoding);
    };

}
