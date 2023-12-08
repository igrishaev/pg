package com.github.igrishaev.util;

import clojure.lang.Keyword;
import clojure.lang.Symbol;
import clojure.lang.Ratio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.github.igrishaev.PGError;
import jsonista.jackson.PersistentVectorDeserializer;
import jsonista.jackson.PersistentHashMapDeserializer;
import jsonista.jackson.KeywordKeyDeserializer;
import jsonista.jackson.KeywordSerializer;
import jsonista.jackson.RatioSerializer;
import jsonista.jackson.SymbolSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class JSON {

    static ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule("postgres");
        module.addDeserializer(List.class, new PersistentVectorDeserializer());
        module.addDeserializer(Map.class, new PersistentHashMapDeserializer());
        module.addSerializer(Keyword.class, new KeywordSerializer(false));
        module.addSerializer(Ratio.class, new RatioSerializer());
        module.addSerializer(Symbol.class, new SymbolSerializer());
        module.addKeyDeserializer(Object.class, new KeywordKeyDeserializer());

        mapper.registerModule(module);
    }

    static Object decodeError(Throwable e) {
        throw new PGError(e, "JSON decode error");
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

    public static void main (String[] args) {
        ByteBuffer buf = ByteBuffer.wrap("[1, 2, 3]".getBytes());
        System.out.println(readValue(buf));
        // System.out.println(readValue("[1, 2, 3, {\"bar/test\": 42, \"foo\": false}]"));
    }



}
