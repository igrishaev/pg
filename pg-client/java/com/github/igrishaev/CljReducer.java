package com.github.igrishaev;

import clojure.lang.Keyword;
import clojure.lang.PersistentVector;
import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentHashMap;
import clojure.lang.ITransientCollection;
import clojure.lang.ITransientAssociative;

public class CljReducer implements IReducer<ITransientCollection, IPersistentCollection> {

    public ITransientCollection initiate() {
        return PersistentVector.EMPTY.asTransient();
    }

    public ITransientCollection append(ITransientCollection acc, Object[] keys, Object[] vals) {
        ITransientAssociative map = PersistentHashMap.EMPTY.asTransient();
        for (int i = 0; i < keys.length; i++) {
            map = map.assoc(keys[i], vals[i]);
        }
        return acc.conj(map.persistent());
    }

    public IPersistentCollection finalize(ITransientCollection acc) {
        return acc.persistent();
    }
}
