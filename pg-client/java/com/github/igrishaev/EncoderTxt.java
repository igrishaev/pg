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

        if (oid == null || oid == OID.TEXT || oid == OID.VARCHAR) {
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
        if (oid == null || oid == OID.INT2 || oid == OID.INT4 || oid == OID.INT8) {
            return x.toString();
        }
        else {
            throw new PGError("Encoding error, value: %s, oid:", x, oid);
        }
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

        if (oid == OID.BOOL || oid == null) {
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
