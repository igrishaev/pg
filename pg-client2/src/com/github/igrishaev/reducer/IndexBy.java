
package com.github.igrishaev.reducer;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.ITransientCollection;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IPersistentCollection;
import clojure.lang.ITransientAssociative;

public class IndexBy implements IReducer<ITransientAssociative, IPersistentCollection> {

    private final IFn f;

    public IndexBy(IFn f) {
        this.f = f;
    }

    public ITransientAssociative initiate () {
        return PersistentHashMap.EMPTY.asTransient();
    }

    public ITransientAssociative append (ITransientAssociative acc, Object[] keys, Object[] vals) {
        Object row = new Object();
        return acc.assoc(f.invoke(row), row);
    }

    public IPersistentCollection finalize (ITransientAssociative acc) {
        return acc.persistent();
    }
}
