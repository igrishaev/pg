package com.github.igrishaev.auth;

import com.github.igrishaev.util.ByteTool;
import com.github.igrishaev.util.HashTool;
import com.github.igrishaev.util.HexTool;
import java.nio.charset.StandardCharsets;

public class MD5 {

    public static String hashPassword (final String user,
                                       final String password,
                                       final byte[] salt
    ) {
        final String credentials = password + user;
        final byte[] input_1 = credentials.getBytes(StandardCharsets.UTF_8);
        final byte[] output_1 = HashTool.MD5encode(input_1);
        final String output_1_hex = HexTool.formatHex(output_1);
        final byte[] input_2 = ByteTool.concat(output_1_hex.getBytes(StandardCharsets.UTF_8), salt);
        byte[] output_2 = HashTool.MD5encode(input_2);
        return "md5" + HexTool.formatHex(output_2);
    }

    public static void main (String[] args) {
        System.out.println(hashPassword("ivan", "secret", new byte[] {1, 2, 3, 4}));
    }

}
