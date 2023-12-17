package com.github.igrishaev;

public class Const {
    public static final int PROTOCOL_VERSION = 196608;
    public static final int CANCEL_CODE = 80877102;
    public static final int SSL_CODE = 80877103;
    public static final int COPY_BUFFER_SIZE = 2048;
    public static final String COPY_FAIL_MSG = "COPY has been interrupted by the client";
    public static final int PG_PORT = 5432;
    public static final String PG_HOST = "127.0.0.1";
    public static final int IN_STREAM_BUF_SIZE = 0xFFFF;
    public static final int OUT_STREAM_BUF_SIZE = 0xFFFF;
    public static final String UTF8 = "UTF-8";
    public static final long EXE_MAX_ROWS = 0xFFFFFFFFL;
    public static final int JSON_ENC_BUF_SIZE = 256;
    public static final String APP_NAME = "pg2";
    public static final String CLIENT_ENCODING = "UTF8";
    public static final char NULL_TAG = (char) 0;
    public static final String COPY_CSV_NULL = "";
    public static final String COPY_CSV_CELL_SEP = ",";
    public static final String COPY_CSV_CELL_QUOTE = "\"";
    public static final String COPY_CSV_LINE_SEP = "\r\n";
    public static final byte[] COPY_BIN_HEADER = {
            // header
            (byte) 'P',
            (byte) 'G',
            (byte) 'C',
            (byte) 'O',
            (byte) 'P',
            (byte) 'Y',
            (byte) 10,
            (byte) 0xFF,
            (byte) 13,
            (byte) 10,
            (byte) 0,
            // 0 int32
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            // 0 int32
            (byte) 0, (byte) 0, (byte) 0, (byte) 0
    };
    public static final byte[] shortMinusOne = new byte[] {(byte) -1, (byte) -1};
}
