package com.github.igrishaev;

import com.github.igrishaev.util.BBTool;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;


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
        BBTool.skip(buf, 2);
        items.add(buf);
        return this;
    }

    public Payload addUnsignedInteger (Long l) {
        size += 4;
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(l);
        BBTool.skip(buf, 4);
        items.add(buf);
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

        ByteBuffer buf;

        if (tag == null) {
            buf = ByteBuffer.allocate(size + 4);
        } else {
            buf = ByteBuffer.allocate(size + 5);
            buf.put((byte)tag.charValue());
        }

        buf.putInt(size + 4);

        for(Object item: items) {
            switch (item) {
                case Integer i:
                    buf.putInt(i);
                    break;
                case Short s:
                    buf.putShort(s);
                    break;
                case ByteBuffer bb:
                    bb.put(bb);
                    break;
                case Byte b:
                    buf.put(b);
                    break;
                case Long l:
                    buf.putLong(l);
                    break;
                case byte[] bs:
                    buf.put(bs);
                    break;
                default:
                    throw new PGError("unsupported item: %s", item);
            }
        }
        return buf;
    }
}
