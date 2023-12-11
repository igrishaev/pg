
package com.github.igrishaev.reducer;

import clojure.lang.IFn;

public class Fold extends MapMixin implements IReducer {

    private final IFn f;
    private final Object init;

    public Fold(final IFn f, final Object init) {
        this.f = f;
        this.init = init;
    }

    public Object initiate () {
        return init;
    }

    public Object append (final Object acc, final Object row) {
        return f.invoke(acc, row);
    }

    public Object finalize (final Object acc) {
        return acc;
    }
}
