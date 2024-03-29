
package com.github.igrishaev.reducer;

import clojure.core$assoc_BANG_;
import clojure.core$persistent_BANG_;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;

import java.util.Objects;

public class IndexBy extends MapMixin implements IReducer {

    private final IFn f;

    public IndexBy(final IFn f) {
        this.f = Objects.requireNonNull(f);
    }

    public Object initiate () {
        return PersistentHashMap.EMPTY.asTransient();
    }

    public Object append (final Object acc, final Object row) {
        return core$assoc_BANG_.invokeStatic(acc, f.invoke(row), row);
    }

    public Object finalize (final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
