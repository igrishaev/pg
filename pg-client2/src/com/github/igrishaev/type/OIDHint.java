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
        if (x == null) {
            return OID.DEFAULT;
        }
        return switch (x.getClass().getCanonicalName()) {
            case "java.lang.Short" -> OID.INT2;
            case "java.lang.Integer" -> OID.INT4;
            case "java.lang.Long" -> OID.INT8;
            case "java.lang." -> OID.FLOAT4;
            case "java.lang.Double" -> OID.FLOAT8;
            case "java.lang.Boolean" -> OID.BOOL;
            case "java.lang.String" -> OID.TEXT;
            case "java.lang.Character" -> OID.TEXT;
            case "clojure.lang.PersistentArrayMap",
                    "clojure.lang.PersistentHashMap" -> OID.JSON;
            case "clojure.lang.Symbol" -> OID.TEXT;
            case "java.util.UUID" -> OID.UUID;
            case "com.github.igrishaev.util.JSON.Wrapper" -> OID.JSON;
            case "byte[]" -> OID.BYTEA;
            case "java.nio.ByteBuffer" -> OID.BYTEA;
            case "java.util.Date" -> OID.TIMESTAMPTZ;
            case "java.time.LocalTime" -> OID.TIME;
            case "java.time.OffsetTime" -> OID.TIMETZ;
            case "java.time.LocalDate" -> OID.DATE;
            case "java.time.LocalDateTime" -> OID.TIMESTAMPTZ;
            case "java.time.OffsetDateTime" -> OID.TIMESTAMPTZ;
            case "java.time.Instant" -> OID.TIMESTAMPTZ;
            case "java.math.BigDecimal"  -> OID.NUMERIC;
            case "java.math.BigInteger" -> OID.NUMERIC;
            case "clojure.lang.BigInt" -> OID.NUMERIC;
            default -> OID.DEFAULT;
        };
    }

}
