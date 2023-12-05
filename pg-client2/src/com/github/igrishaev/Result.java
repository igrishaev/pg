package com.github.igrishaev;

public record Result(String tag,
                     int selectedCount,
                     int insertedCount,
                     int updatedCount,
                     int deletedCount,
                     int copyCount,
                     Object result) {}
