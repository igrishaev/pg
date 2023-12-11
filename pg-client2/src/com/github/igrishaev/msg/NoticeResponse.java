package com.github.igrishaev.msg;

import clojure.lang.*;
import com.github.igrishaev.util.IClojure;

import java.nio.ByteBuffer;
import java.util.Map;

public record NoticeResponse(Map<String, String> fields) implements IClojure {

    public Associative toClojure () {
        Associative map = PersistentHashMap.EMPTY;
        for (Map.Entry<String, String> e: fields.entrySet()) {
            map = RT.assoc(map, Keyword.intern(e.getKey()), e.getValue());
        }
        return map;
    }

    public static NoticeResponse fromByteBuffer (ByteBuffer buf) {
        // TODO encoding
        Map<String, String> fields = FieldParser.parseFields(buf, "UTF-8");
        return new NoticeResponse(fields);
    }
}
