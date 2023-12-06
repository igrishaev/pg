
package com.github.igrishaev.reducer;

import clojure.core$assoc_BANG_;
import clojure.core$persistent_BANG_;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;

public class KV extends MapMixin implements IReducer {

    private final IFn fk, fv;

    public KV(IFn fk, IFn fv) {
        this.fk = fk;
        this.fv = fv;
    }

    public Object initiate () {
        return PersistentHashMap.EMPTY.asTransient();
    }

    public Object append (Object acc, Object row) {
        return core$assoc_BANG_.invokeStatic(acc, fk.invoke(row), fv.invoke(row));
    }

    public Object finalize (Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
