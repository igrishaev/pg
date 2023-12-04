package com.github.igrishaev.msg;

import java.nio.ByteBuffer;

public interface IMessage {
    ByteBuffer encode(String encoding);
}
