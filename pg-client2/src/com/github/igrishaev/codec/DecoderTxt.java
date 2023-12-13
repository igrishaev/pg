package com.github.igrishaev.codec;

import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.JSON;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import java.nio.ByteBuffer;
import java.time.temporal.ChronoField;
import java.util.UUID;
import java.math.BigDecimal;

public class DecoderTxt extends ACodec {

    private static final DateTimeFormatter frmt_timestamptz;

    static {
        frmt_timestamptz = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .appendPattern("x")
                .toFormatter()
                .withZone(ZoneOffset.UTC);
    }

    public Object decode(ByteBuffer buf, OID oid) {

        return switch (oid) {

            case INT2 -> Short.parseShort(getString(buf));
            case INT4, OID -> Integer.parseInt(getString(buf));
            case INT8 -> Long.parseLong(getString(buf));
            case BYTEA -> buf.array();
            case CHAR -> buf.getChar();
            case UUID -> UUID.fromString(getString(buf));
            case FLOAT4 -> Float.parseFloat(getString(buf));
            case FLOAT8 -> Double.parseDouble(getString(buf));
            case NUMERIC -> new BigDecimal(getString(buf));
            case BOOL -> {
                byte b = buf.get();
                yield switch ((char) b) {
                    case 't' -> true;
                    case 'f' -> false;
                    default -> throw new PGError("wrong boolean value: %s", b);
                };
            }
            case JSON, JSONB -> JSON.readValue(buf);

            case TIMESTAMPTZ -> OffsetDateTime.parse(
                    getString(buf), frmt_timestamptz
            );
            
            default -> getString(buf);
        };
    }

}
