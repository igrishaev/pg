
package com.github.igrishaev.reducer;

import clojure.core$assoc_BANG_;
import clojure.core$persistent_BANG_;
import clojure.lang.Obj;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;

import java.util.Objects;

public class KV extends MapMixin implements IReducer {

    private final IFn fk, fv;

    public KV(final IFn fk, final IFn fv) {
        this.fk = Objects.requireNonNull(fk);
        this.fv = Objects.requireNonNull(fv);
    }

    public Object initiate () {
        return PersistentHashMap.EMPTY.asTransient();
    }

    public Object append (final Object acc, final Object row) {
        return core$assoc_BANG_.invokeStatic(acc, fk.invoke(row), fv.invoke(row));
    }

    public Object finalize (final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
