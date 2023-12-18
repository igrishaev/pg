package com.github.igrishaev.msg;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import com.github.igrishaev.util.BBTool;
import com.github.igrishaev.util.IClojure;

import java.nio.ByteBuffer;

public record NotificationResponse(int pid,
                                   String channel,
                                   String message)
    implements IClojure {

    public IPersistentMap toClojure () {
        return PersistentHashMap.create(
                Keyword.intern("msg"), Keyword.intern("NoticeResponse"),
                Keyword.intern("pid"), pid,
                Keyword.intern("channel"), channel,
                Keyword.intern("message"), message
        );
    }

    public static NotificationResponse fromByteBuffer (ByteBuffer buf) {
        // TODO
        final int pid = buf.getInt();
        final String channel = BBTool.getCString(buf, "UTF-8");
        final String message = BBTool.getCString(buf, "UTF-8");
        return new NotificationResponse(pid, channel, message);
    }
}
