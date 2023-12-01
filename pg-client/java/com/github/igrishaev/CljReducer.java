package com.github.igrishaev;

import clojure.lang.RT;
import clojure.lang.PersistentVector;
import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentHashMap;

public class CljReducer implements IReducer<IPersistentCollection, IPersistentCollection> {
    public IPersistentCollection initiate() {
        return PersistentVector.EMPTY;
    }
    public IPersistentCollection append(IPersistentCollection acc, Object[] keys, Object[] values) {
        PersistentHashMap row = PersistentHashMap.create(1, 2, 3, 4);
        return RT.conj(acc, row);
    }
    public IPersistentCollection finalize(IPersistentCollection acc) {
        return acc;
    }
}
