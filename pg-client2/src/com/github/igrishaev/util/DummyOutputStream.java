package com.github.igrishaev.util;

import java.io.IOException;
import java.io.OutputStream;

public class DummyOutputStream extends OutputStream {
    public void write(int b) throws IOException {}
}
