package com.github.igrishaev;

import clojure.lang.RT;
import clojure.lang.PersistentVector;
import clojure.lang.IPersistentCollection;

public class CljReducer implements IReducer<IPersistentCollection, IPersistentCollection> {
    public IPersistentCollection initiate() {
        return PersistentVector.EMPTY;
    }
    public IPersistentCollection append(IPersistentCollection acc, Object row) {
        return RT.conj(acc, row);
    }
    public IPersistentCollection finalize(IPersistentCollection acc) {
        return acc;
    }
}
