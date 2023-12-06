package com.github.igrishaev.reducer;

import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;
import clojure.lang.PersistentVector;

public class Matrix extends MapMixin implements IReducer {

    @Override
    public Object compose(Object[] keys, Object[] vals) {
        return PersistentVector.create(vals);
    }

    public Object initiate() {
        return PersistentVector.EMPTY.asTransient();
    }

    public Object append(Object acc, Object row) {
        return core$conj_BANG_.invokeStatic(acc, row);
    }

    public Object finalize(Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
