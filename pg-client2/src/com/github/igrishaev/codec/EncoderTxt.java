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
import java.util.UUID;
import java.math.BigDecimal;
import java.math.BigInteger;
import clojure.lang.BigInt;
import com.github.igrishaev.util.HexTool;
import com.github.igrishaev.util.JSON;

public class EncoderTxt {

    public static String encode(final Object x) {
        return encode(x, OID.DEFAULT, CodecParams.standard());
    }

    public static String encode(final Object x, final OID oid) {
        return encode(x, oid, CodecParams.standard());
    }

    public static String encode(final Object x, final CodecParams codecParams) {
        return encode(x, OID.DEFAULT, codecParams);
    }

    private static String txtEncodingError(final Object x, final OID oid) {
        throw new PGError(
                "cannot text-encode a value: %s, OID: %s, type: %s",
                x, oid, x.getClass().getCanonicalName());
    }

    public static String encode(final Object x, final OID oid, final CodecParams codecParams) {

        if (x == null) {
            throw new PGError("cannot text-encode a null value");
        }

        return switch (x.getClass().getCanonicalName()) {

            case "clojure.lang.Symbol" -> switch (oid) {
                case TEXT, VARCHAR, DEFAULT -> x.toString();
                default -> txtEncodingError(x, oid);
            };

            case "java.lang.Character" -> switch (oid) {
                case TEXT, VARCHAR, CHAR, DEFAULT -> x.toString();
                default -> txtEncodingError(x, oid);
            };

            case "java.lang.String" -> switch (oid) {
                case TEXT, VARCHAR, NAME, JSON, JSONB, DEFAULT -> (String)x;
                default -> txtEncodingError(x, oid);
            };

            case "java.lang.Short",
                    "java.lang.Integer",
                    "java.lang.Long"-> switch (oid) {
                case INT2, INT4, INT8, OID, NUMERIC, DEFAULT -> x.toString();
                default -> txtEncodingError(x, oid);
            };

            case "byte[]" -> switch (oid) {
                case BYTEA, DEFAULT -> HexTool.formatHex((byte[])x, "\\x");
                default -> txtEncodingError(x, oid);
            };


            case "java.lang.Float",
                    "java.lang.Double"-> switch (oid) {
                case FLOAT4, FLOAT8, DEFAULT -> x.toString();
                default -> txtEncodingError(x, oid);
            };

            case "java.util.UUID" -> switch (oid) {
                case UUID, TEXT, VARCHAR, DEFAULT -> x.toString();
                default -> txtEncodingError(x, oid);
            };

            case "java.lang.Boolean" -> switch (oid) {
                case BOOL, DEFAULT -> (boolean)x ? "t" : "f";
                default -> txtEncodingError(x, oid);
            };

            case "java.math.BigDecimal" -> switch (oid) {
                case NUMERIC, FLOAT4, FLOAT8, DEFAULT -> x.toString();
                default -> txtEncodingError(x, oid);
            };

            case "java.math.BigInteger",
                    "clojure.lang.BigInt" -> switch (oid) {
                case INT2, INT4, INT8, FLOAT4, FLOAT8, NUMERIC, DEFAULT -> x.toString();
                default -> txtEncodingError(x, oid);
            };

            case "com.github.igrishaev.util.JSON.Wrapper" -> switch (oid) {
                case JSON, JSONB, DEFAULT -> {
                    // TODO: maybe return bytes?
                    // TODO: guess the initial size?
                    final StringWriter writer = new StringWriter(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(writer, ((JSON.Wrapper)x).value());
                    yield writer.toString();
                }
                default -> txtEncodingError(x, oid);
            };

            case "clojure.lang.PersistentArrayMap",
                    "clojure.lang.PersistentHashMap" -> switch (oid) {
                // TODO: maybe return bytes?
                // TODO: guess the initial size?
                case JSON, JSONB, DEFAULT -> {
                    final StringWriter writer = new StringWriter(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(writer, x);
                    yield writer.toString();
                }
                default -> txtEncodingError(x, oid);
            };

            case "java.util.Date" -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(LocalDate.ofInstant(((Date)x).toInstant(), ZoneOffset.UTC));
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(((Date)x).toInstant());
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(((Date)x).toInstant());
                default -> txtEncodingError(x, oid);
            };

            case "java.time.OffsetTime" -> switch (oid) {
                case TIME -> DateTimeTxt.encodeTIME(((OffsetTime)x).toLocalTime());
                case TIMETZ, DEFAULT -> DateTimeTxt.encodeTIMETZ(((OffsetTime)x));
                default -> txtEncodingError(x, oid);
            };

            case "java.time.LocalTime" -> switch (oid) {
                case TIME, DEFAULT -> DateTimeTxt.encodeTIME(((LocalTime)x));
                case TIMETZ -> DateTimeTxt.encodeTIMETZ(((LocalTime)x).atOffset(ZoneOffset.UTC));
                default -> txtEncodingError(x, oid);
            };

            case "java.time.LocalDate" -> switch (oid) {
                case DATE, DEFAULT -> DateTimeTxt.encodeDATE(((LocalDate)x));
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(
                        ((LocalDate)x).atStartOfDay(ZoneOffset.UTC).toInstant()
                );
                case TIMESTAMPTZ -> DateTimeTxt.encodeTIMESTAMPTZ(
                        ((LocalDate)x).atStartOfDay(ZoneOffset.UTC).toInstant()
                );
                default -> txtEncodingError(x, oid);
            };

            case "java.time.LocalDateTime" -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(((LocalDateTime)x).toLocalDate());
                case TIMESTAMP, DEFAULT -> DateTimeTxt.encodeTIMESTAMP(((LocalDateTime)x).toInstant(ZoneOffset.UTC));
                case TIMESTAMPTZ -> DateTimeTxt.encodeTIMESTAMPTZ(((LocalDateTime)x).toInstant(ZoneOffset.UTC));
                default -> txtEncodingError(x, oid);
            };

            case "java.time.ZonedDateTime" -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(((ZonedDateTime)x).toLocalDate());
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(((ZonedDateTime)x));
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(((ZonedDateTime)x));
                default -> txtEncodingError(x, oid);
            };

            case "java.time.OffsetDateTime" -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(((OffsetDateTime)x).toLocalDate());
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(((OffsetDateTime)x));
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(((OffsetDateTime)x));
                default -> txtEncodingError(x, oid);
            };

            case "java.time.Instant" -> switch (oid) {
                case DATE -> DateTimeTxt.encodeDATE(LocalDate.ofInstant((Instant)x, ZoneOffset.UTC));
                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP((Instant)x);
                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ((Instant)x);
                default -> txtEncodingError(x, oid);
            };

            default -> txtEncodingError(x, oid);
        };
    }

    public static void main (final String[] args) {
        System.out.println(encode("hello".getBytes(StandardCharsets.UTF_8)));
    }
}
