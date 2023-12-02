package com.github.igrishaev;

import clojure.lang.RT;
import clojure.lang.PersistentVector;
import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentHashMap;
import clojure.lang.IPersistentMap;

public class CljReducer implements IReducer<IPersistentCollection, IPersistentCollection> {
    public PersistentVector initiate() {
        return PersistentVector.EMPTY;
    }
    public IPersistentCollection append(IPersistentCollection acc, Object[] keys, Object[] vals) {
        IPersistentMap map = PersistentHashMap.EMPTY;
        for (int i = 0; i < keys.length; i++) {
            map = map.assoc(keys[i], vals[i]);
        }
        return RT.conj(acc, map);
    }
    public IPersistentCollection finalize(IPersistentCollection acc) {
        return acc;
    }
}
