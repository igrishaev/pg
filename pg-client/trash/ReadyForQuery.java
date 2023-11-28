package com.github.igrishaev;

import java.nio.ByteBuffer;

public class ReadyForQuery {

    private char txStatus;

    public ReadyForQuery (ByteBuffer buf) {
        txStatus = buf.getChar();
    };

    public char getTxStatus() {
        return txStatus;
    };

}
