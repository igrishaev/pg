package com.github.igrishaev.msg;

import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;

public record NotificationResponse(int pid,
                                   String channel,
                                   String message) {

    public static NotificationResponse fromByteBuffer (ByteBuffer buf) {
        // TODO
        final int pid = buf.getInt();
        final String channel = BBTool.getCString(buf, "UTF-8");
        final String message = BBTool.getCString(buf, "UTF-8");
        return new NotificationResponse(pid, channel, message);
    }
}
