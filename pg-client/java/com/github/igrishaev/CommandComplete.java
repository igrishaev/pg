package com.github.igrishaev;

import java.nio.ByteBuffer;

public record CommandComplete (String tag) {
        public CommandComplete (ByteBuffer buf) {
                this(BBUtil.getCString(buf, "UTF-8"));
        }
}
