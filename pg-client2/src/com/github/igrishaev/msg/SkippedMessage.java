package com.github.igrishaev.msg;

public record SkippedMessage() {
    public static SkippedMessage INSTANCE = new SkippedMessage();
}
