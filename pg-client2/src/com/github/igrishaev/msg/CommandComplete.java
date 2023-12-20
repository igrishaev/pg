package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record CommandComplete (String command) {
        public static CommandComplete fromByteBuffer(final ByteBuffer buf, final Charset charset) {
                String command = BBTool.getCString(buf, charset);
                return new CommandComplete(command);
        }
}
