package com.github.igrishaev;

import java.nio.ByteBuffer;


public class Parser {

    private String encoding;

    public static Object parse (char tag, ByteBuffer buf) {

        switch (tag) {

        case 'A':
            return new DataRow(buf);

        case 'B':
            return new CommandComplete(buf);

        default:
            throw new Error("aaa");

        }

    }


}
