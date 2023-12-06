package com.github.igrishaev.reducer;

public interface IReducer {
    String[] unifyKeys(String[] keys);
    Object transformKey(String key);
    Object compose(Object[] keys, Object[] vals);
    Object initiate();
    Object append(Object acc, Object row);
    Object finalize(Object acc);
}
