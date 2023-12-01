package com.github.igrishaev;

import java.nio.ByteBuffer;

public record AuthenticationResponse (int status) {
    public static AuthenticationResponse fromByteBuffer(ByteBuffer buf) {
        int status = buf.getInt();
        return new AuthenticationResponse(status);
    }
    public Object parseResponse (ByteBuffer buf) {
        return switch (status) {
            case  0 -> new AuthenticationOk();
            case  3 -> new AuthenticationCleartextPassword();
            case  5 -> AuthenticationMD5Password.fromByteBuffer(buf);
            case 10 -> AuthenticationSASL.fromByteBuffer(buf);
            case 11 -> AuthenticationSASLContinue.fromByteBuffer(buf);
            case 12 -> AuthenticationSASLFinal.fromByteBuffer(buf);
            default -> throw new PGError(
                    "Unknown auth response message, status: %s",
                    status
            );
        };
    }
}
