package com.github.igrishaev;

import java.util.ArrayList;


public class API {

    static Object query (Connection conn, String sql) {
        conn.sendQuery(sql);
        return Flow.interact(conn, "query");
    }

}
