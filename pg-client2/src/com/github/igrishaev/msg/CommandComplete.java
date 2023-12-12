package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;

public record CommandComplete (String command) {
        public static CommandComplete fromByteBuffer(ByteBuffer buf) {
                String command = BBTool.getCString(buf, "UTF-8");
                return new CommandComplete(command);
        }
}
