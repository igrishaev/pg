package com.github.igrishaev.auth;

import com.github.igrishaev.PGError;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public class MD5 {

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static MessageDigest getMD5 () {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new PGError(e, "cannot find MD5 MessageDigest");
        }
    }

    public static byte[] MD5encode (byte[] input) {
        MessageDigest md5 = getMD5();
        md5.update(input);
        return md5.digest();
    }

    public static String hashPassword (final String user,
                                       final String password,
                                       final byte[] salt) {

        HexFormat hex = HexFormat.of();

        final String credentials = password + user;
        final byte[] input_1 = credentials.getBytes(StandardCharsets.UTF_8);
        final byte[] output_1 = MD5encode(input_1);
        final String output_1_hex = hex.formatHex(output_1);

        final byte[] input_2 = concat(output_1_hex.getBytes(StandardCharsets.UTF_8), salt);
        byte[] output_2 = MD5encode(input_2);

        return "md5" + hex.formatHex(output_2);
    }

    public static void main (String[] args) {
        System.out.println(hashPassword("ivan", "secret", new byte[] {1, 2, 3, 4}));
    }

}
