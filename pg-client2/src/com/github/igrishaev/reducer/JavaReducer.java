package com.github.igrishaev.reducer;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class JavaReducer implements IReducer<List<HashMap<Object, Object>>, List<HashMap<Object, Object>>> {

    public List<HashMap<Object, Object>> initiate () {
        return new ArrayList<>();
    }

    public List<HashMap<Object, Object>> append (List<HashMap<Object, Object>> acc, Object[] keys, Object[] vals) {
        HashMap<Object, Object> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], vals[i]);
        }
        acc.add(map);
        return acc;
    }

    public List<HashMap<Object, Object>> finalize (List<HashMap<Object, Object>> acc) {
        return acc;
    }
}
