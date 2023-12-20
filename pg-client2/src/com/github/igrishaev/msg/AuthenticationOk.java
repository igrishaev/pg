package com.github.igrishaev.msg;

public record AuthenticationOk() {
    public static Integer status = 0;
    public static AuthenticationOk INSTANCE = new AuthenticationOk();
}
