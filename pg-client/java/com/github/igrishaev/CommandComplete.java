package com.github.igrishaev;

import java.nio.ByteBuffer;

public class CommandComplete {

    public final String tag;

    public CommandComplete (ByteBuffer buf) {
        tag = BBUtil.getCString(buf, "UTF-8");
    }

}
