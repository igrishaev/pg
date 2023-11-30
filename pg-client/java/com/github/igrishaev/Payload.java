package com.github.igrishaev;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;


public class Payload {

    private Integer size;
    private final ArrayList<Object> items;

    public Payload() {
        size = 0;
        items = new ArrayList<>();
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

    public Payload addBytes(byte[] buf) {
        size += buf.length;
        items.add(buf);
        return this;
    }

    public Payload addUnsignedShort (Integer i) {
        size += 2;

        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(i);

        byte[] bytes = Arrays.copyOfRange(buf.array(), 2, 4);
        items.add(bytes);

        return this;
    }

    public Payload addUnsignedInteger (Long l) {

        size += 4;

        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(l);

        byte[] bytes = Arrays.copyOfRange(buf.array(), 4, 8);
        items.add(bytes);

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
            switch (item) {
                case Integer i:
                    bb.putInt(i); break;
                case Short s:
                    bb.putShort(s); break;
                case Byte b:
                    bb.put(b); break;
                case Long l:
                    bb.putLong(l); break;
                case byte[] bs:
                    bb.put(bs); break;
                default:
                    throw new PGError("unsupported item: %s", item);
            }
        }
        return bb;
    }
}
