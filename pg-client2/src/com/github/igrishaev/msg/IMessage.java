package com.github.igrishaev.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface IMessage {
    ByteBuffer encode(Charset encoding);
}
