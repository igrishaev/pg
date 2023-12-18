package com.github.igrishaev.util;

import com.github.igrishaev.PGError;

import java.io.IOException;
import java.io.InputStream;

public class IOTool {
    public static int read(InputStream inputStream, byte[] buf) {
        try {
            return inputStream.read(buf);
        } catch (IOException e) {
            throw new PGError(e, "cannot read from the input stream");
        }
    }

    public static int read(InputStream inputStream, byte[] buf, int offset, int len) {
        try {
            return inputStream.read(buf, offset, len);
        } catch (IOException e) {
            throw new PGError(e, "cannot read from the input stream");
        }
    }
}
