package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class ErrorResponse {

    public final HashMap<String, String> fields;

    public String parseTag (byte tag) {
        switch ((char) tag) {
        case 'S': return "severity";
        case 'V': return "verbosity";
        case 'C': return "code";
        case 'M': return "message";
        case 'D': return "detail";
        case 'H': return "hint";
        case 'P': return "position";
        case 'p': return "position-internal";
        case 'q': return "query";
        case 'W': return "stacktrace";
        case 's': return "schema";
        case 't': return "table";
        case 'c': return "column";
        case 'd': return "datatype";
        case 'n': return "constraint";
        case 'F': return "file";
        case 'L': return "line";
        case 'R': return "function";
        default: throw new PGError("unknown tag: %s", tag);
        }
    }

    public ErrorResponse (ByteBuffer buf) {

        fields = new HashMap<String, String>();

        while (true) {
            byte tag = buf.get();
            if (tag == 0) {
                break;
            }
            else {
                String field = parseTag(tag);
                String message = BBUtil.getCString(buf, "UTF-8");
                fields.put(field, message);
            }

        }

    }

}
