package com.github.igrishaev.util;

import clojure.lang.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.igrishaev.PGError;
import jsonista.jackson.PersistentVectorDeserializer;
import jsonista.jackson.PersistentHashMapDeserializer;
import jsonista.jackson.KeywordKeyDeserializer;
import jsonista.jackson.KeywordSerializer;
import jsonista.jackson.RatioSerializer;
import jsonista.jackson.SymbolSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


public class JSON {

    static ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule("pg");
        module.addDeserializer(List.class, new PersistentVectorDeserializer());
        module.addDeserializer(Map.class, new PersistentHashMapDeserializer());
        module.addSerializer(Keyword.class, new KeywordSerializer(false));
        module.addKeySerializer(Keyword.class, new KeywordSerializer(true));
        module.addKeyDeserializer(Object.class, new KeywordKeyDeserializer());
        module.addSerializer(Ratio.class, new RatioSerializer());
        module.addSerializer(Symbol.class, new SymbolSerializer());
        module.addKeyDeserializer(Object.class, new KeywordKeyDeserializer());

        mapper.registerModule(module);
    }

    static Object decodeError(Throwable e) {
        throw new PGError(e, "JSON decode error");
    }

    static void encodeError(Throwable e, Object value) {
        throw new PGError(e, "JSON encode error, value: %s", value);
    }

    public static Object readValue (String input) {
        try {
            return mapper.readValue(input, Object.class);
        } catch (JsonProcessingException e) {
            return decodeError(e);
        }
    }

    public static Object readValueBinary (ByteBuffer buf) {
        byte b = buf.get();
        if (b == 1) {
            buf.limit(buf.limit() - 1);
        }
        else {
            buf.position(buf.position() - 1);
        }
        return readValue(buf);
    }

    public static Object readValue (ByteBuffer buf) {
        int offset = buf.arrayOffset() + buf.position();
        int len = buf.limit();
        try {
            return mapper.readValue(buf.array(), offset, len, Object.class);
        } catch (IOException e) {
            return decodeError(e);
        }
    }

    public static void writeValue (OutputStream outputStream, Object value) {
        try {
            mapper.writeValue(outputStream, value);
        } catch (IOException e) {
            encodeError(e, value);
        }
    }

    public static void writeValue (Writer writer, Object value) {
        try {
            mapper.writeValue(writer, value);
        } catch (IOException e) {
            encodeError(e, value);
        }
    }

    public static void main (String[] args) {
        ByteBuffer buf = ByteBuffer.wrap("[1, 2, 3]".getBytes());
        System.out.println(readValue(buf));

        PersistentVector vector = PersistentVector.create(
                1,
                Keyword.intern("foo", "bar"),
                Keyword.intern("no-namespace"),
                true,
                null,
                PersistentVector.create("nested", 42),
                PersistentHashMap.create(
                        "key1", 42,
                        Keyword.intern("aaa", "bbb"), 100,
                        Keyword.intern("aaa"), Symbol.intern("foo", "bar")
                )
        );

        // System.out.println(vector);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeValue(out, vector);
        System.out.println(out.toString(StandardCharsets.UTF_8));
        System.out.println(readValue(out.toString(StandardCharsets.UTF_8)));

    }



}
