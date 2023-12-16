package com.github.igrishaev.codec;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.time.temporal.Temporal;
import java.util.UUID;
import java.util.Date;

import clojure.lang.IPersistentCollection;
import clojure.lang.Symbol;
import com.github.igrishaev.Const;
import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.JSON;

public class EncoderBin {

    public static ByteBuffer encode (Object x) {
        return encode(x, OID.DEFAULT, new CodecParams());
    }

    public static ByteBuffer encode (Object x, CodecParams codecParams) {
        return encode(x, OID.DEFAULT, codecParams);
    }

    public static ByteBuffer encode (Object x, OID oid) {
        return encode(x, oid, new CodecParams());
    }

    private static ByteBuffer binEncodingError(Object x, OID oid) {
        throw new PGError("cannot binary-encode a value: %s, OID: %s", x, oid);
    }

    private static byte[] getBytes (String string, CodecParams codecParams) {
        try {
            return string.getBytes(codecParams.clientEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new PGError(e, "could not get bytes from a string");
        }
    }

    public static ByteBuffer encode (Object x, OID oid, CodecParams codecParams) {

        return switch (x) {

            case Symbol s -> switch (oid) {
                case TEXT, VARCHAR, DEFAULT -> {
                    byte[] bytes = getBytes(s.toString(), codecParams);
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case String s -> switch (oid) {
                case TEXT, VARCHAR, NAME, DEFAULT -> {
                    byte[] bytes = getBytes(s, codecParams);
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case Character c -> switch (oid) {
                case TEXT, VARCHAR, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(2);
                    buf.putChar(c);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case Short s -> switch (oid) {
                case INT2, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(2);
                    buf.putShort(s);
                    yield buf;
                }
                case INT4 -> {
                    ByteBuffer buf = ByteBuffer.allocate(4);
                    buf.putInt(s);
                    yield buf;
                }
                case INT8 -> {
                    ByteBuffer buf = ByteBuffer.allocate(8);
                    buf.putLong(s);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case Integer i -> switch (oid) {
                case INT2 -> {
                    ByteBuffer buf = ByteBuffer.allocate(2);
                    buf.putShort(i.shortValue());
                    yield buf;
                }
                case INT4, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(4);
                    buf.putInt(i);
                    yield buf;
                }
                case INT8 -> {
                    ByteBuffer buf = ByteBuffer.allocate(8);
                    buf.putLong(i);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case Long l -> switch (oid) {
                case INT2 -> {
                    ByteBuffer buf = ByteBuffer.allocate(2);
                    buf.putShort(l.shortValue());
                    yield buf;
                }
                case INT4 -> {
                    ByteBuffer buf = ByteBuffer.allocate(4);
                    buf.putInt(l.intValue());
                    yield buf;
                }
                case INT8, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(8);
                    buf.putLong(l);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case Boolean b -> switch (oid) {
                case BOOL, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(1);
                    buf.put(b ? (byte)1 : (byte)0);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case UUID u -> switch (oid) {
                case UUID, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(16);
                    buf.putLong(u.getMostSignificantBits());
                    buf.putLong(u.getLeastSignificantBits());
                    yield buf;
                }
                case TEXT, VARCHAR -> {
                    byte[] bytes = getBytes(u.toString(), codecParams);
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case Float f -> switch (oid) {
                case FLOAT4, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(4);
                    buf.putFloat(f);
                    yield buf;
                }
                case FLOAT8 -> {
                    ByteBuffer buf = ByteBuffer.allocate(8);
                    buf.putDouble(f);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case Double d -> switch (oid) {
                case FLOAT4 -> {
                    float f = d.floatValue();

                    if (Float.isInfinite(f)) {
                        throw new PGError("double->float coercion let to an infinite value: %s", d);
                    }
                    if (Float.isNaN(f)) {
                        throw new PGError("double->float coercion let to an NAN value: %s", d);
                    }

                    ByteBuffer buf = ByteBuffer.allocate(4);
                    buf.putFloat(f);
                    yield buf;
                }
                case FLOAT8, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(8);
                    buf.putDouble(d);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case JSON.Wrapper w -> switch (oid) {
                case JSON, JSONB, DEFAULT -> {
                    // TODO; guess the size?
                    ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(out, w.value());
                    yield ByteBuffer.wrap(out.toByteArray());
                }
                default -> binEncodingError(w.value(), oid);
            };

            case IPersistentCollection c -> switch (oid) {
                case JSON, JSONB, DEFAULT -> {
                    // TODO; guess the size?
                    ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(out, c);
                    yield ByteBuffer.wrap(out.toByteArray());
                }
                default -> binEncodingError(x, oid);
            };

            case Date d -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(d);
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(d);
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ(d);
                default -> binEncodingError(d, oid);
            };

            // TODO: split on types
            // TODO: DEFAULT
            case Temporal t -> switch (oid) {
                case TIME -> DateTimeBin.encodeTIME(t);
                case TIMETZ -> DateTimeBin.encodeTIMETZ(t);
                case DATE -> DateTimeBin.encodeDATE(t);
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(t);
                case TIMESTAMPTZ -> DateTimeBin.encodeTIMESTAMPTZ(t);
                default -> binEncodingError(t, oid);
            };

            // TODO: BigDecimal, BigInteger, BigInt

            default -> binEncodingError(x, oid);
        };
    }
}
