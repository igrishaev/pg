package com.github.igrishaev.codec;

import clojure.lang.Symbol;
import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;

public class EncoderTxt extends ACodec {

    public String encodingError(Object x, OID oid) {
        throw new PGError("cannot text-encode a value: %s, OID: %s", x, oid);
    }

    public String encode(Object x, OID oid) {
        return switch (x) {

            case String s -> {
                switch (oid) {
                    case TEXT, VARCHAR: yield s;
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

            // case Long l -> "2222";

            default -> throw new PGError("aaa");
        };
    }

    // Symbol
    public String encode(Symbol x, OID oid) {
        return switch (oid) {
            case TEXT, VARCHAR -> x.toString();
            default -> encodingError(x, oid);
        };
    }

    // Character
    public String encode(char x, OID oid) {
        return switch (oid) {
            case CHAR, TEXT, VARCHAR -> String.valueOf(x);
            default -> encodingError(x, oid);
        };
    }

    // String
//    public String encode(String x, OID oid) {
//        return switch (oid) {
//            case TEXT, VARCHAR -> x;
//            default -> encodingError(x, oid);
//        };
//        if (oid == null || oid == OID.INT2 || oid == OID.INT4 || oid == OID.INT8) {
//            return x.toString();
//        }
//        else {
//            throw new PGError("Encoding error, value: %s, oid:", x, oid);
//        }
//    }

    //
    // Long
    //


    //
    // Integer
    //

    //
    // Short
    //

    //
    // Double
    //

    //
    // Float
    //

    //
    // BigDecimal
    //

    //
    // BigInteger
    //

    //
    // BigInt
    //

    //
    // Boolean
    //
    public String encode(Boolean x) {
        return encode(x, null);
    }

    public String encode(Boolean x, OID oid) {
        return switch (oid) {
            case OID.BOOL -> x ? "t" : "f";
            default -> throw new PGError("Encoding error, value: %s, oid:", x, oid);
        };
    }

    //
    // OID
    //

    //
    // Name
    //

    //
    // UUID
    //

}
