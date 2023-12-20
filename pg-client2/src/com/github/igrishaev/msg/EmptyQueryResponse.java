package com.github.igrishaev.msg;

public record EmptyQueryResponse() {
    public static EmptyQueryResponse INSTANCE = new EmptyQueryResponse();
}
