package com.github.igrishaev.msg;

import com.github.igrishaev.BBUtil;
import com.github.igrishaev.PGError;

import java.nio.ByteBuffer;
import java.util.HashMap;

public record ErrorResponse (HashMap<String, String> fields) {

    public static String parseTag (byte tag) {
        return switch ((char) tag) {
            case 'S' -> "severity";
            case 'V' -> "verbosity";
            case 'C' -> "code";
            case 'M' -> "message";
            case 'D' -> "detail";
            case 'H' -> "hint";
            case 'P' -> "position";
            case 'p' -> "position-internal";
            case 'q' -> "query";
            case 'W' -> "stacktrace";
            case 's' -> "schema";
            case 't' -> "table";
            case 'c' -> "column";
            case 'd' -> "datatype";
            case 'n' -> "constraint";
            case 'F' -> "file";
            case 'L' -> "line";
            case 'R' -> "function";
            default -> throw new PGError("unknown tag: %s", tag);
        };
    }

    public static ErrorResponse fromByteBuffer(ByteBuffer buf) {
        HashMap<String, String> fields = new HashMap<>();
        while (true) {
            byte tag = buf.get();
            if (tag == 0) {
                break;
            }
            else {
                String field = parseTag(tag);
                String message = BBUtil.getCString(buf, "UTF-8");
                fields.put(field, message);
            };
        };
        return new ErrorResponse(fields);
    }
}
