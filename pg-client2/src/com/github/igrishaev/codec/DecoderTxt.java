package com.github.igrishaev.codec;

import com.github.igrishaev.PGError;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.util.JSON;

import java.util.Arrays;
import java.util.HexFormat;

import java.util.UUID;
import java.math.BigDecimal;

public class DecoderTxt {

    private static final HexFormat hex = HexFormat.of();

    public static Object decode(
            final String string,
            final OID oid
    ) {

        return switch (oid) {

            case INT2 -> Short.parseShort(string);
            case INT4, OID -> Integer.parseInt(string);
            case INT8 -> Long.parseLong(string);
            case BYTEA -> hex.parseHex(string, 2, string.length());
            case CHAR -> string.charAt(0);
            case UUID -> UUID.fromString(string);
            case FLOAT4 -> Float.parseFloat(string);
            case FLOAT8 -> Double.parseDouble(string);
            case NUMERIC -> new BigDecimal(string);
            case BOOL -> switch (string) {
                    case "t" -> true;
                    case "f" -> false;
                    default -> throw new PGError("wrong boolean value: %s", string);
            };
            case JSON, JSONB -> JSON.readValue(string);
            case TIMESTAMPTZ -> DateTimeTxt.decodeTIMESTAMPTZ(string);
            case TIMESTAMP -> DateTimeTxt.decodeTIMESTAMP(string);
            case DATE -> DateTimeTxt.decodeDATE(string);
            case TIMETZ -> DateTimeTxt.decodeTIMETZ(string);
            case TIME -> DateTimeTxt.decodeTIME(string);
            default -> string;
        };
    }

    public static void main (final String[] args) {
        final String string = "\\xDEADBEEF";
        System.out.println(Arrays.toString(HexFormat.of().parseHex(string, 2, string.length())));
    }

}
