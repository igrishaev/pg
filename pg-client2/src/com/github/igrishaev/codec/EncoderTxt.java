package com.github.igrishaev.codec;

import clojure.lang.Symbol;
import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.BigInteger;
import clojure.lang.BigInt;

public class EncoderTxt extends ACodec {

    public String encodingError(Object x, OID oid) {
        throw new PGError("cannot text-encode a value: %s, OID: %s", x, oid);
    }

    public String encode(Object x, OID oid) {
        return switch (x) {

            case Symbol s -> {
                switch (oid) {
                    case TEXT, VARCHAR: yield s.toString();
                    default: yield encodingError(x, oid);
                }
            }

            case String s -> {
                switch (oid) {
                    case TEXT, VARCHAR: yield s;
                    default: yield encodingError(x, oid);
                }
            }

            case Short s -> {
                switch (oid) {
                    case INT2: yield String.valueOf(s);
                    case INT4: yield String.valueOf(s.intValue());
                    case INT8: yield String.valueOf(s.longValue());
                    default: yield encodingError(x, oid);
                }
            }

            case Integer i -> {
                switch (oid) {
                    case INT2: yield String.valueOf(i.shortValue());
                    case INT4: yield String.valueOf(i);
                    case INT8: yield String.valueOf(i.longValue());
                    default: yield encodingError(x, oid);
                }
            }

            case Long l -> {
                switch (oid) {
                    case INT2: yield String.valueOf(l.shortValue());
                    case INT4: yield String.valueOf(l.intValue());
                    case INT8: yield String.valueOf(l);
                    default: yield encodingError(x, oid);
                }
            }

            case Float f -> {
                switch (oid) {
                    case FLOAT4: yield String.valueOf(f);
                    case FLOAT8: yield String.valueOf(f.doubleValue());
                    default: yield encodingError(x, oid);
                }
            }

            case Double d -> {
                switch (oid) {
                    case FLOAT4: yield String.valueOf(d.floatValue());
                    case FLOAT8: yield String.valueOf(d);
                    default: yield encodingError(x, oid);
                }
            }

            case UUID u -> {
                switch (oid) {
                    case TEXT, VARCHAR: yield String.valueOf(u);
                    default: yield encodingError(x, oid);
                }
            }

            case Boolean b -> {
                switch (oid) {
                    case BOOL: yield b ? "t" : "f";
                    default: yield encodingError(x, oid);
                }
            }

            case BigDecimal bd -> {
                switch (oid) {
                    case NUMERIC, FLOAT4, FLOAT8: yield bd.toString();
                    default: yield encodingError(x, oid);
                }
            }

            case BigInteger bi -> {
                switch (oid) {
                    case INT2, INT4, INT8: yield bi.toString();
                    default: yield encodingError(x, oid);
                }
            }

            case BigInt bi -> {
                switch (oid) {
                    case INT2, INT4, INT8: yield bi.toString();
                    default: yield encodingError(x, oid);
                }
            }

            default -> encodingError(x, oid);
        };
    }
}
