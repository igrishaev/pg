package com.github.igrishaev.reducer;

public class Dummy implements IReducer {

    public String[] unifyKeys(String[] keys) {
        return keys;
    }

    public Object transformKey(String key) {
        return key;
    }

    public Object compose(Object[] keys, Object[] vals) {
        return null;
    }

    public Object initiate() {
        return null;
    }

    public Object append(Object acc, Object row) {
        return acc;
    }

    public Object finalize(Object acc) {
        return acc;
    }
}
