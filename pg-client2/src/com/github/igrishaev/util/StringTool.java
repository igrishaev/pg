package com.github.igrishaev.util;

import java.nio.charset.StandardCharsets;

public class StringTool {

    public static byte[] getBytes (String line) {
        return line.getBytes(StandardCharsets.UTF_8);
    }
}
