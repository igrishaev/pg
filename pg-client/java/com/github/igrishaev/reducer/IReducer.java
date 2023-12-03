package com.github.igrishaev.reducer;

public interface IReducer<I, R> {
    I initiate();
    I append(I acc, Object[] keys, Object[] values);
    R finalize(I acc);
}
