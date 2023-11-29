package com.github.igrishaev;

import java.nio.ByteBuffer;

public class ReadyForQuery {

    public final byte txStatus;

    public ReadyForQuery(ByteBuffer buf) {
        txStatus = buf.get();
    }

}
