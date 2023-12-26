package com.github.igrishaev.type;

import clojure.lang.BigInt;
import clojure.lang.IPersistentCollection;
import clojure.lang.Symbol;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.JSON;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.UUID;
import java.util.Date;

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
            case IPersistentCollection ignored -> OID.JSON;
            case Symbol ignored -> OID.TEXT;
            case UUID ignored -> OID.UUID;
            case JSON.Wrapper ignored -> OID.JSON;
            case byte[] ignored -> OID.BYTEA;
            case ByteBuffer ignored -> OID.BYTEA;
            case Date ignored -> OID.TIMESTAMPTZ;
            case LocalTime ignored -> OID.TIME;
            case OffsetTime ignored -> OID.TIMETZ;
            case LocalDate ignored -> OID.DATE;
            case LocalDateTime ignored -> OID.TIMESTAMPTZ;
            case OffsetDateTime ignored -> OID.TIMESTAMPTZ;
            case Instant ignored -> OID.TIMESTAMPTZ;
            case BigDecimal ignored -> OID.NUMERIC;
            case BigInteger ignored -> OID.NUMERIC;
            case BigInt ignored -> OID.NUMERIC;
            case null, default -> OID.DEFAULT;
        };
    }

}
