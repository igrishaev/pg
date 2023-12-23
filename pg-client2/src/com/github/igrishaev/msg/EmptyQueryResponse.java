package com.github.igrishaev.msg;

public record EmptyQueryResponse() {
    public final static EmptyQueryResponse INSTANCE = new EmptyQueryResponse();
}
