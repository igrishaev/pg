package com.github.igrishaev.reducer;

public class First extends MapMixin implements IReducer {

    public Object initiate () {
        return new Object[1];
    }

    public Object append (final Object obj, final Object row) {
        Object[] acc = (Object[]) obj;
        if (acc[0] == null) {
            acc[0] = row;
        }
        return acc;
    }

    public Object finalize (final Object obj) {
        Object[] acc = (Object[]) obj;
        return acc[0];
    }
}
