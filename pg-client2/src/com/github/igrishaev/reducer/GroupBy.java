
package com.github.igrishaev.reducer;

import clojure.lang.RT;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;
import clojure.lang.PersistentVector;

public class GroupBy extends MapMixin implements IReducer {

    private final IFn f;

    public GroupBy(IFn f) {
        this.f = f;
    }

    public Object initiate () {
        return PersistentHashMap.EMPTY;
    }

    public Object append (Object acc, Object row) {
        Object key = f.invoke(row);
        if (RT.contains(acc, key) == Boolean.FALSE) {
            return RT.assoc(acc, key, PersistentVector.EMPTY.cons(key));
        }
        else {
            PersistentVector val = (PersistentVector) RT.get(acc, key);
            return RT.assoc(acc, key, val.cons(key));
        }
    }

    public Object finalize (Object acc) {
        return acc;
    }
}
