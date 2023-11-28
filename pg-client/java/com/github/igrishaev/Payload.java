package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;

public class Payload {

    private Integer size;
    private ArrayList<Object> items;

    public Payload() {
        size = 0;
        items = new ArrayList<Object>();
    }

    public Payload addInteger(Integer i) {
        size += 4;
        items.add(i);
        return this;
    }

    public Payload addShort(Short s) {
        size += 2;
        items.add(s);
        return this;
    }

    public Payload addByte(Byte b) {
        size += 1;
        items.add(b);
        return this;
    }

    public Payload addCString(String s) {
        return addCString(s, "UTF-8");
    }

    public Payload addCString(String s, String encoding) {

        try {
            byte[] bytes = s.getBytes(encoding);
            size = size + bytes.length + 1;
            items.add(bytes);
            items.add((byte)0);
            return this;
        }
        catch (UnsupportedEncodingException e) {
            throw new PGError(e, "cannot get bytes for a C-string");
        }

    }

    public ByteBuffer toByteBuffer(Character tag) {

        ByteBuffer bb;

        if (tag == null) {
            bb = ByteBuffer.allocate(size + 4);
        } else {
            bb = ByteBuffer.allocate(size + 5);
            bb.put((byte)tag.charValue());
        }

        bb.putInt(size + 4);

        for(Object item: items) {

            if (item instanceof Integer) {
                bb.putInt((int) item);

            } else if (item instanceof Short) {
                bb.putShort((short) item);

            } else if (item instanceof Byte) {
                bb.put((byte) item);

            } else if (item instanceof Long) {
                bb.putLong((long) item);

            } else if (item instanceof byte[]) {
                bb.put((byte[]) item);

            } else {
                throw new PGError("unsupported item: %s", item);
            }
        }

        return bb;

    }

}
