package com.github.igrishaev.reducer;

public class Dummy implements IReducer {

    public Object compose(final Object[] keys, final Object[] vals) {
        return null;
    }

    public Object initiate() {
        return null;
    }

    public Object append(final Object acc, final Object row) {
        return acc;
    }

    public Object finalize(final Object acc) {
        return acc;
    }
}
