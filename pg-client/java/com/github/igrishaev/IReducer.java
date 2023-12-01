package com.github.igrishaev;

public interface IReducer<T, R> {
    T initiate();
    T append(T acc, Object[] keys, Object[] values);
    R finalize(T acc);
}
