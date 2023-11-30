package com.github.igrishaev;

import java.nio.ByteBuffer;

public interface IMessage {
    ByteBuffer encode(String encoding);

}
