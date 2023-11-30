package com.github.igrishaev;

import java.nio.ByteBuffer;
import clojure.lang.Symbol;


public class EncoderBin {

    //
    // Symbol
    //

    public ByteBuffer encode(Symbol x) {
        return encode(x, null);
    }

    public ByteBuffer encode(Symbol x, Integer oid) {

        if (oid == null || oid == OID.TEXT || oid == OID.VARCHAR) {
            return ByteBuffer.allocate(4);
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

    // public ByteBuffer encode(Long x) {
    //     return encode(x, null);
    // }

    // public ByteBuffer encode(Long x, Integer oid) {
    //     if (oid == null || oid == OID.int2 || oid == OID.int4 || oid == OID.int8) {
    //         return x.toString();
    //     }
    //     else {
    //         throw new PGError("Encoding error, value: %s, oid:", x, oid);
    //     }
    // }

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
    // public ByteBuffer encode(Boolean x) {
    //     return encode(x, null);
    // }

    // public ByteBuffer encode(Boolean x, Integer oid) {

    //     if (oid == OID.bool || oid == null) {
    //         if (x) {
    //             return "t";
    //         }
    //         else {
    //             return "f";
    //         }
    //     }
    //     else {
    //         throw new PGError("Encoding error, value: %s, oid:", x, oid);
    //     }

    // }

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