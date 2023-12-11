package com.github.igrishaev.msg;

import java.nio.ByteBuffer;
import java.util.Map;

public record ErrorResponse (Map<String, String> fields) {

    public static ErrorResponse fromByteBuffer(ByteBuffer buf) {
        // TODO: encoding
        Map<String, String> fields = FieldParser.parseFields(buf, "UTF-8");
        return new ErrorResponse(fields);
    }
}
