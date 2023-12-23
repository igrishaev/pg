package com.github.igrishaev.auth;

import com.github.igrishaev.util.ByteTool;
import com.github.igrishaev.util.Codec;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class MD5 {

    private static final HexFormat hex = HexFormat.of();

    public static String hashPassword (final String user,
                                       final String password,
                                       final byte[] salt
    ) {
        final String credentials = password + user;
        final byte[] input_1 = credentials.getBytes(StandardCharsets.UTF_8);
        final byte[] output_1 = Codec.MD5encode(input_1);
        final String output_1_hex = hex.formatHex(output_1);
        final byte[] input_2 = ByteTool.concat(output_1_hex.getBytes(StandardCharsets.UTF_8), salt);
        byte[] output_2 = Codec.MD5encode(input_2);
        return "md5" + hex.formatHex(output_2);
    }

    public static void main (String[] args) {
        System.out.println(hashPassword("ivan", "secret", new byte[] {1, 2, 3, 4}));
    }

}
