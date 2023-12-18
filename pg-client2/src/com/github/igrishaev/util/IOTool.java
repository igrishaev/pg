package com.github.igrishaev.util;

import com.github.igrishaev.PGError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOTool {

    public static int read(
            final InputStream inputStream,
            final byte[] buf) {
        try {
            return inputStream.read(buf);
        } catch (IOException e) {
            throw new PGError(e, "cannot read from the input stream");
        }
    }

    public static int read(
            final InputStream inputStream,
            final byte[] buf,
            final int offset,
            int len) {
        try {
            return inputStream.read(buf, offset, len);
        } catch (IOException e) {
            throw new PGError(e, "cannot read from the input stream");
        }
    }

    public static void write(final OutputStream outputStream, final byte[] buf) {
        try {
            outputStream.write(buf);
        } catch (IOException e) {
            throw new PGError(e, "cannot write a byte array into an output stream");
        }
    }

    public static byte[] readNBytes(final InputStream inputStream, final int len) {
        try {
            return inputStream.readNBytes(len);
        }
        catch (IOException e) {
            throw new PGError(e, "cannot read N bytes from a stream, len: %s", len);
        }
    }

    public static long skip(final InputStream inputStream, final long n) {
        try {
            return inputStream.skip(n);
        }
        catch (IOException e) {
            throw new PGError(e, "cannot skip N bytes from a stream, n: %s", n);
        }
    }

    public static void flush(final OutputStream outputStream) {
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw new PGError(e, "cannot flush an output stream");
        }
    }

    public static void write(final OutputStream outputStream,
                             final byte[] buf,
                             final int offset,
                             final int len) {
        try {
            outputStream.write(buf, offset, len);
        } catch (IOException e) {
            throw new PGError(
                    e,
                    "cannot write a byte array into an output stream, offset: %s, len: %s",
                    offset, len
            );
        }
    }
}
