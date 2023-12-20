package com.github.igrishaev.msg;

public record AuthenticationCleartextPassword () {
    public static Integer status = 3;
    public static AuthenticationCleartextPassword INSTANCE = new AuthenticationCleartextPassword();
}
