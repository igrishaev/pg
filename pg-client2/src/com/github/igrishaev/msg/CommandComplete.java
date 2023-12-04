package com.github.igrishaev.msg;

import com.github.igrishaev.BBUtil;

import java.nio.ByteBuffer;

public record CommandComplete (String tag) {
        public static CommandComplete fromByteBuffer(ByteBuffer buf) {
                String tag = BBUtil.getCString(buf, "UTF-8");
                return new CommandComplete(tag);
        }
}
