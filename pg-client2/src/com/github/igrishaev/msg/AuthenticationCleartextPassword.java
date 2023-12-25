package com.github.igrishaev.msg;

public record AuthenticationCleartextPassword () {

    public static final int status = 3;

    public static final AuthenticationCleartextPassword INSTANCE = new AuthenticationCleartextPassword();
}
