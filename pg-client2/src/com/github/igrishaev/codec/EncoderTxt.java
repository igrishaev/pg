package com.github.igrishaev.codec;

import clojure.lang.Symbol;
import clojure.lang.IPersistentCollection;
import com.github.igrishaev.Const;
import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;
import java.io.StringWriter;
import java.util.HexFormat;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.BigInteger;
import clojure.lang.BigInt;
import com.github.igrishaev.util.JSON;

public class EncoderTxt {

    private static final HexFormat hex = HexFormat.of();

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
        throw new PGError(
                "cannot text-encode a value: %s, OID: %s, type: %s",
                x, oid, x.getClass().getCanonicalName());
    }

    public static String encode(Object x, OID oid, CodecParams codecParams) {
        return switch (x) {

            case null -> throw new PGError("cannot text-encode a null value");

            case Symbol s -> switch (oid) {
                case TEXT, VARCHAR, DEFAULT -> s.toString();
                default -> txtEncodingError(x, oid);
            };

            case Character c -> switch (oid) {
                case TEXT, VARCHAR, DEFAULT -> c.toString();
                default -> txtEncodingError(x, oid);
            };

            case String s -> switch (oid) {
                case TEXT, VARCHAR, NAME, JSON, JSONB, DEFAULT -> s;
                default -> txtEncodingError(x, oid);
            };

            case Short s -> switch (oid) {
                case INT2, DEFAULT -> String.valueOf(s);
                case INT4, OID -> String.valueOf(s.intValue());
                case INT8 -> String.valueOf(s.longValue());
                default -> txtEncodingError(x, oid);
            };

            case byte[] ba -> switch (oid) {
                case BYTEA, DEFAULT -> {
                    final int len = 2 + 2 * ba.length;
                    final StringBuilder sb = new StringBuilder(len);
                    sb.append("\\x");
                    yield hex.formatHex(sb, ba).toString();
                }
                default -> txtEncodingError(x, oid);
            };

            case Integer i -> switch (oid) {
                case INT2 -> String.valueOf(i.shortValue());
                case INT4, OID, DEFAULT -> String.valueOf(i);
                case INT8 -> String.valueOf(i.longValue());
                default -> txtEncodingError(x, oid);
            };

            case Long l -> switch (oid) {
                case INT2 -> String.valueOf(l.shortValue());
                case INT4, OID -> String.valueOf(l.intValue());
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

            case Date d -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(LocalDate.ofInstant(d.toInstant(), ZoneOffset.UTC));
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(d.toInstant());
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(d.toInstant());
                default -> txtEncodingError(x, oid);
            };

            case OffsetTime ot -> switch (oid) {
                case TIME -> DateTimeTxt.encodeTIME(ot.toLocalTime());
                case TIMETZ, DEFAULT -> DateTimeTxt.encodeTIMETZ(ot);
                default -> txtEncodingError(x, oid);
            };

            case LocalTime lt -> switch (oid) {
                case TIME, DEFAULT -> DateTimeTxt.encodeTIME(lt);
                case TIMETZ -> DateTimeTxt.encodeTIMETZ(lt.atOffset(ZoneOffset.UTC));
                default -> txtEncodingError(x, oid);
            };

            case LocalDate ld -> switch (oid) {
                case DATE, DEFAULT -> DateTimeTxt.encodeDATE(ld);
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(
                        ld.atStartOfDay(ZoneOffset.UTC).toInstant()
                );
                case TIMESTAMPTZ -> DateTimeTxt.encodeTIMESTAMPTZ(
                        ld.atStartOfDay(ZoneOffset.UTC).toInstant()
                );
                default -> txtEncodingError(x, oid);
            };

            case LocalDateTime ldt -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(ldt.toLocalDate());
                case TIMESTAMP, DEFAULT -> DateTimeTxt.encodeTIMESTAMP(ldt.toInstant(ZoneOffset.UTC));
                case TIMESTAMPTZ -> DateTimeTxt.encodeTIMESTAMPTZ(ldt.toInstant(ZoneOffset.UTC));
                default -> txtEncodingError(x, oid);
            };

            case ZonedDateTime zdt -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(zdt.toLocalDate());
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(zdt);
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(zdt);
                default -> txtEncodingError(x, oid);
            };

            case OffsetDateTime odt -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(odt.toLocalDate());
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(odt);
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(odt);
                default -> txtEncodingError(x, oid);
            };

            case Instant i -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(LocalDate.ofInstant(i, ZoneOffset.UTC));
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(i);
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(i);
                default -> txtEncodingError(x, oid);
            };

            default -> txtEncodingError(x, oid);
        };
    }

    public static void main (String[] args) {
        System.out.println(encode("hello".getBytes(StandardCharsets.UTF_8)));
    }
}
