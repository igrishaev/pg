
package com.github.igrishaev.reducer;

import clojure.lang.IFn;

public class Run extends MapMixin implements IReducer {

    private final IFn f;

    public Run(IFn f) {
        this.f = f;
    }

    public Object initiate () {
        return 0;
    }

    public Object append (Object acc, Object row) {
        f.invoke(row);
        return (Integer) acc + 1;
    }

    public Object finalize (Object acc) {
        return acc;
    }
}
