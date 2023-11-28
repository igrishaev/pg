package com.github.igrishaev;

import clojure.lang.Symbol;


public class EncoderTxt {

    //
    // Symbol
    //
    public String encode(Symbol x) {
        return encode(x, null);
    }

    public String encode(Symbol x, Integer oid) {

        if (oid == null || oid == OID.text || oid == OID.varchar) {
            return x.toString();
        }
        else {
            throw new PGError("Encoding error, value: %s, oid:", x, oid);
        }

    }

    //
    // String
    //

    //
    // Character
    //

    //
    // Long
    //

    public String encode(Long x) {
        return encode(x, null);
    }

    public String encode(Long x, Integer oid) {
        return "100500";
    }

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

    public String encode(Boolean x, Integer oid) {

        if (oid == OID.bool || oid == null) {
            if (x) {
                return "t";
            }
            else {
                return "f";
            }
        }
        else {
            throw new PGError("Encoding error, value: %s, oid:", x, oid);
        }

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
