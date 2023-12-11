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
    public static long EXE_MAX_ROWS = 0xFFFFFFFFL;
    public static int JSON_ENC_BUF_SIZE = 256;
    public static String APP_NAME = "pg2";
    public static String CLIENT_ENCODING = "UTF8";
    public static char NULL_TAG = (char) 0;
}
