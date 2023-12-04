package com.github.igrishaev;

public class Const {
    static final int PROTOCOL_VERSION = 196608;
    static final int CANCEL_CODE = 80877102;
    static final int SSL_CODE = 80877103;
    static final int COPY_BUFFER_SIZE = 2048;
    static final String COPY_FAIL_MSG = "COPY has been interrupted by the client";
    static final int PG_PORT = 5432;
    static final String PG_HOST = "127.0.0.1";
    static final int IN_STREAM_BUF_SIZE = 0xFFFF;
    static final int OUT_STREAM_BUF_SIZE = 0xFFFF;
    static final String UTF8 = "UTF-8";
}
