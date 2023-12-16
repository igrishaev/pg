package com.github.igrishaev.codec;

import clojure.lang.Symbol;
import clojure.lang.IPersistentCollection;
import com.github.igrishaev.Const;
import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.temporal.Temporal;
import java.util.Date;
import java.io.StringWriter;
import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.BigInteger;
import clojure.lang.BigInt;
import com.github.igrishaev.util.JSON;

public class EncoderTxt {

    public static String encode(Object x) {
        return encode(x, OID.DEFAULT, CodecParams.standard());
    }

    public static String encode(Object x, OID oid) {
        return encode(x, oid, CodecParams.standard());
    }

    public static String encode(Object x, CodecParams codecParams) {
        return encode(x, OID.DEFAULT, codecParams);
    }

    private static String txtEncodingError(Object x, OID oid) {
        throw new PGError("cannot text-encode a value: %s, OID: %s", x, oid);
    }

    public static String encode(Object x, OID oid, CodecParams codecParams) {
        return switch (x) {

            case Symbol s -> switch (oid) {
                case TEXT, VARCHAR, DEFAULT -> s.toString();
                default -> txtEncodingError(x, oid);
            };

            case String s -> switch (oid) {
                case TEXT, VARCHAR, NAME, DEFAULT -> s;
                default -> txtEncodingError(x, oid);
            };

            case Short s -> switch (oid) {
                case INT2, DEFAULT -> String.valueOf(s);
                case INT4 -> String.valueOf(s.intValue());
                case INT8 -> String.valueOf(s.longValue());
                default -> txtEncodingError(x, oid);
            };

            case Integer i -> switch (oid) {
                case INT2 -> String.valueOf(i.shortValue());
                case INT4, DEFAULT -> String.valueOf(i);
                case INT8 -> String.valueOf(i.longValue());
                default -> txtEncodingError(x, oid);
            };

            case Long l -> switch (oid) {
                case INT2 -> String.valueOf(l.shortValue());
                case INT4 -> String.valueOf(l.intValue());
                case INT8, DEFAULT -> String.valueOf(l);
                default -> txtEncodingError(x, oid);
            };

            case Float f -> switch (oid) {
                case FLOAT4, DEFAULT -> String.valueOf(f);
                case FLOAT8 -> String.valueOf(f.doubleValue());
                default -> txtEncodingError(x, oid);
            };

            case Double d -> switch (oid) {
                case FLOAT4 -> String.valueOf(d.floatValue());
                case FLOAT8, DEFAULT -> String.valueOf(d);
                default -> txtEncodingError(x, oid);
            };

            case UUID u -> switch (oid) {
                case UUID, TEXT, VARCHAR, DEFAULT -> String.valueOf(u);
                default -> txtEncodingError(x, oid);
            };

            case Boolean b -> switch (oid) {
                case BOOL, DEFAULT -> b ? "t" : "f";
                default -> txtEncodingError(x, oid);
            };

            case BigDecimal bd -> switch (oid) {
                case NUMERIC, FLOAT4, FLOAT8, DEFAULT -> bd.toString();
                default -> txtEncodingError(x, oid);
            };

            case BigInteger bi -> switch (oid) {
                case INT2, INT4, INT8, DEFAULT -> bi.toString();
                default -> txtEncodingError(x, oid);
            };

            case BigInt bi -> switch (oid) {
                case INT2, INT4, INT8, DEFAULT -> bi.toString();
                default -> txtEncodingError(x, oid);
            };

            case JSON.Wrapper w -> switch (oid) {
                case JSON, JSONB, DEFAULT -> {
                    // TODO: maybe return bytes?
                    // TODO: guess the initial size?
                    StringWriter writer = new StringWriter(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(writer, w.value());
                    yield writer.toString();
                }
                default -> txtEncodingError(w.value(), oid);
            };

            case IPersistentCollection c -> switch (oid) {
                // TODO: maybe return bytes?
                // TODO: guess the initial size?
                case JSON, JSONB, DEFAULT -> {
                    StringWriter writer = new StringWriter(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(writer, c);
                    yield writer.toString();
                }
                default -> txtEncodingError(c, oid);
            };

            // TODO: split on types
            // TODO: defaults
            case Temporal t -> switch (oid) {
                case TIMESTAMPTZ -> DateTimeTxt.encodeTIMESTAMPTZ(t);
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(t);
                case DATE -> DateTimeTxt.encodeDATE(t);
                case TIMETZ -> DateTimeTxt.encodeTIMETZ(t);
                case TIME -> DateTimeTxt.encodeTIME(t);
                default -> txtEncodingError(t, oid);
            };

            case Date d -> switch (oid) {
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(d);
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(d);
                case DATE -> DateTimeTxt.encodeDATE(d);
                default -> txtEncodingError(d, oid);
            };

            default -> txtEncodingError(x, oid);
        };
    }
}
