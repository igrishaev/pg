package com.github.igrishaev.reducer;

import clojure.lang.Keyword;
import clojure.lang.ITransientAssociative;
import clojure.lang.PersistentHashMap;

public abstract class MapMixin implements IReducer {

    public String[] unifyKeys(String[] keys) {
        return keys;
    }
    public Keyword transformKey(String key) {
        return Keyword.intern(key);
    }
    public Object compose(Object[] keys, Object[] vals) {
        ITransientAssociative map = PersistentHashMap.EMPTY.asTransient();
        for (short i = 0; i < keys.length; i++) {
            map = map.assoc(keys[i], vals[i]);
        }
        return map.persistent();
    }

}
