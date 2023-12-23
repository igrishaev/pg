package com.github.igrishaev.msg;

import com.github.igrishaev.enums.SASL;
import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public record AuthenticationSASL (HashSet<SASL> SASLTypes) {

    public static final int status = 10;

    public static AuthenticationSASL fromByteBuffer(final ByteBuffer buf) {
        final HashSet<SASL> types = new HashSet<>();
        while (true) {
            final String type = BBTool.getCString(buf, StandardCharsets.UTF_8);
            if (type.isEmpty()) {
                break;
            }
            types.add(SASL.ofCode(type));
        }
        return new AuthenticationSASL(types);
    }

    public boolean isScramSha256 () {
        return SASLTypes.contains(SASL.SCRAM_SHA_256);
    }

    public boolean isScramSha256Plus () {
        return SASLTypes.contains(SASL.SCRAM_SHA_256_PLUS);
    }

}
