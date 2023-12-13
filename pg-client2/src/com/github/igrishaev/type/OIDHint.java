package com.github.igrishaev.type;

import clojure.lang.Symbol;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.JSON;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public class OIDHint {

    public static OID guessOID (Object x) {
        return switch (x) {
            case Short ignored -> OID.INT2;
            case Integer ignored -> OID.INT4;
            case Long ignored -> OID.INT8;
            case Float ignored -> OID.FLOAT4;
            case Double ignored -> OID.FLOAT8;
            case Boolean ignored -> OID.BOOL;
            case String ignored -> OID.TEXT;
            case Character ignored -> OID.TEXT;
            case Map<?,?> ignored -> OID.JSON;
            case Symbol ignored -> OID.TEXT;
            case UUID ignored -> OID.UUID;
            case JSON.Wrapper ignored -> OID.JSON;
            case byte[] ignored -> OID.BYTEA;
            case ByteBuffer ignored -> OID.BYTEA;
            default -> OID.DEFAULT;
        };
    }

}
