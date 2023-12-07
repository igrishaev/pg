package com.github.igrishaev.codec;

import java.nio.ByteBuffer;
import java.util.UUID;

import clojure.lang.Symbol;
import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;

public class EncoderBin extends ACodec {

    public ByteBuffer encode (Object x, OID oid) {

        return switch (x) {

            case Symbol s -> switch (oid) {
                case TEXT, VARCHAR -> {
                    byte[] bytes = getBytes(s.toString());
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case String s -> switch (oid) {
                case TEXT, VARCHAR -> {
                    byte[] bytes = getBytes(s);
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case Character c -> switch (oid) {
                case TEXT, VARCHAR -> {
                    ByteBuffer buf = ByteBuffer.allocate(2);
                    buf.putChar(c);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case Short s -> switch (oid) {
                case INT2 -> {
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
                case INT4 -> {
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
                case INT8 -> {
                    ByteBuffer buf = ByteBuffer.allocate(8);
                    buf.putLong(l);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case Boolean b -> switch (oid) {
                case BOOL -> {
                    ByteBuffer buf = ByteBuffer.allocate(1);
                    buf.put(b ? (byte)1 : (byte)0);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case UUID u -> switch (oid) {
                case UUID -> {
                    ByteBuffer buf = ByteBuffer.allocate(16);
                    buf.putLong(u.getMostSignificantBits());
                    buf.putLong(u.getLeastSignificantBits());
                    yield buf;
                }
                case TEXT, VARCHAR -> {
                    byte[] bytes = getBytes(u.toString());
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case Float f -> switch (oid) {
                case FLOAT4 -> {
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
                case FLOAT8 -> {
                    ByteBuffer buf = ByteBuffer.allocate(8);
                    buf.putDouble(d);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            // BigDecimal, BigInteger, BigInt

            default -> binEncodingError(x, oid);
        };
    }
}
