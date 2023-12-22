package com.github.igrishaev.codec;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;
import java.math.BigInteger;
import clojure.lang.BigInt;

import clojure.lang.IPersistentCollection;
import clojure.lang.Symbol;
import com.github.igrishaev.Const;
import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.BBTool;
import com.github.igrishaev.util.JSON;

public class EncoderBin {

    public static ByteBuffer encode (Object x) {
        return encode(x, OID.DEFAULT, CodecParams.standard());
    }

    public static ByteBuffer encode (Object x, CodecParams codecParams) {
        return encode(x, OID.DEFAULT, codecParams);
    }

    public static ByteBuffer encode (Object x, OID oid) {
        return encode(x, oid, CodecParams.standard());
    }

    private static ByteBuffer binEncodingError(Object x, OID oid) {
        throw new PGError(
                "cannot binary-encode a value: %s, OID: %s, type: %s",
                x, oid, x.getClass().getCanonicalName()
        );
    }

    private static byte[] getBytes (String string, CodecParams codecParams) {
        return string.getBytes(codecParams.clientCharset);
    }

    public static ByteBuffer encode (Object x, OID oid, CodecParams codecParams) {

        return switch (x) {

            case null -> throw new PGError("cannot binary-encode a null value");

            case Symbol s -> switch (oid) {
                case TEXT, VARCHAR, DEFAULT -> {
                    byte[] bytes = getBytes(s.toString(), codecParams);
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case String s -> switch (oid) {
                case TEXT, VARCHAR, NAME, JSON, JSONB, DEFAULT -> {
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
                case INT2, DEFAULT -> BBTool.ofShort(s);
                case INT4 -> BBTool.ofInt(s);
                case INT8 -> BBTool.ofLong(s);
                case FLOAT4 -> BBTool.ofFloat(s);
                case FLOAT8 -> BBTool.ofDouble(s);
                default -> binEncodingError(x, oid);
            };

            case Integer i -> switch (oid) {
                case INT2 -> BBTool.ofShort(i.shortValue());
                case INT4, DEFAULT -> BBTool.ofInt(i);
                case INT8 -> BBTool.ofLong(i);
                case FLOAT4 -> BBTool.ofFloat(i);
                case FLOAT8 -> BBTool.ofDouble(i);
                default -> binEncodingError(x, oid);
            };

            case Long l -> switch (oid) {
                case INT2 -> BBTool.ofShort(l.shortValue());
                case INT4 -> BBTool.ofInt(l.intValue());
                case INT8, DEFAULT -> BBTool.ofLong(l);
                case FLOAT4 -> BBTool.ofFloat(l);
                case FLOAT8 -> BBTool.ofDouble(l);
                default -> binEncodingError(x, oid);
            };

            case Byte b -> switch (oid) {
                case INT2, DEFAULT -> BBTool.ofShort(b);
                case INT4 -> BBTool.ofInt(b);
                case INT8 -> BBTool.ofLong(b);
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
                case FLOAT4, DEFAULT -> BBTool.ofFloat(f);
                case FLOAT8 -> BBTool.ofDouble(f);
                default -> binEncodingError(x, oid);
            };

            case Double d -> switch (oid) {
                case FLOAT4 -> {
                    float f = d.floatValue();

                    if (Float.isInfinite(f)) {
                        throw new PGError("double->float coercion led to an infinite value: %s", d);
                    }
                    if (Float.isNaN(f)) {
                        throw new PGError("double->float coercion led to a NAN value: %s", d);
                    }

                    yield BBTool.ofFloat(f);
                }
                case FLOAT8, DEFAULT -> BBTool.ofDouble(d);
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
                case DATE -> DateTimeBin.encodeDATE(LocalDate.ofInstant(d.toInstant(), ZoneOffset.UTC));
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(d.toInstant());
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ(d.toInstant());
                default -> binEncodingError(x, oid);
            };

            case OffsetTime ot -> switch (oid) {
                case TIME -> DateTimeBin.encodeTIME(ot.toLocalTime());
                case TIMETZ, DEFAULT -> DateTimeBin.encodeTIMETZ(ot);
                default -> binEncodingError(x, oid);
            };

            case LocalTime lt -> switch (oid) {
                case TIME, DEFAULT -> DateTimeBin.encodeTIME(lt);
                case TIMETZ -> DateTimeBin.encodeTIMETZ(lt.atOffset(ZoneOffset.UTC));
                default -> binEncodingError(x, oid);
            };

            case LocalDate ld -> switch (oid) {
                case DATE, DEFAULT -> DateTimeBin.encodeDATE(ld);
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(
                        ld.atStartOfDay(ZoneOffset.UTC).toInstant()
                );
                case TIMESTAMPTZ -> DateTimeBin.encodeTIMESTAMPTZ(
                        ld.atStartOfDay(ZoneOffset.UTC).toInstant()
                );
                default -> binEncodingError(x, oid);
            };

            case LocalDateTime ldt -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(ldt.toLocalDate());
                case TIMESTAMP, DEFAULT -> DateTimeBin.encodeTIMESTAMP(ldt.toInstant(ZoneOffset.UTC));
                case TIMESTAMPTZ -> DateTimeBin.encodeTIMESTAMPTZ(ldt.toInstant(ZoneOffset.UTC));
                default -> binEncodingError(x, oid);
            };

            case ZonedDateTime zdt -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(zdt.toLocalDate());
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(zdt);
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ(zdt);
                default -> binEncodingError(x, oid);
            };

            case OffsetDateTime odt -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(odt.toLocalDate());
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(odt);
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ(odt);
                default -> binEncodingError(x, oid);
            };

            case Instant i -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(LocalDate.ofInstant(i, ZoneOffset.UTC));
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(i);
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ(i);
                default -> binEncodingError(x, oid);
            };

            case BigDecimal bd -> switch (oid) {
                case NUMERIC, DEFAULT -> NumericBin.encode(bd);
                case INT2 -> BBTool.ofShort(bd.shortValueExact());
                case INT4 -> BBTool.ofInt(bd.intValueExact());
                case INT8 -> BBTool.ofLong(bd.longValueExact());
                case FLOAT4 -> BBTool.ofFloat(bd.floatValue());
                case FLOAT8 -> BBTool.ofDouble(bd.doubleValue());
                default -> binEncodingError(x, oid);
            };

            case BigInteger bi -> switch (oid) {
                case NUMERIC, DEFAULT -> NumericBin.encode(new BigDecimal(bi));
                case INT2 -> BBTool.ofShort(bi.shortValueExact());
                case INT4 -> BBTool.ofInt(bi.intValueExact());
                case INT8 -> BBTool.ofLong(bi.longValueExact());
                case FLOAT4 -> BBTool.ofFloat(bi.floatValue());
                case FLOAT8 -> BBTool.ofDouble(bi.doubleValue());
                default -> binEncodingError(x, oid);
            };

            case BigInt bi -> switch (oid) {
                case NUMERIC, DEFAULT -> NumericBin.encode(bi.toBigDecimal());
                case INT2 -> BBTool.ofShort(bi.shortValue());
                case INT4 -> BBTool.ofInt(bi.intValue());
                case INT8 -> BBTool.ofLong(bi.longValue());
                case FLOAT4 -> BBTool.ofFloat(bi.floatValue());
                case FLOAT8 -> BBTool.ofDouble(bi.doubleValue());
                default -> binEncodingError(x, oid);
            };

            default -> binEncodingError(x, oid);
        };
    }
}
