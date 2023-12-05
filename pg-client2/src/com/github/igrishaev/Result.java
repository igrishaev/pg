package com.github.igrishaev;

public record Result(String tag,
                     int rowsProcessed,
                     Object result) {}
