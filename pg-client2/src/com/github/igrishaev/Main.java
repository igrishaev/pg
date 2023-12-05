package com.github.igrishaev;

import java.util.ArrayList;

public class Main {

    public static void main (String[] args) {
        String user = System.getenv("USER");
        Connection conn = new Connection("127.0.0.1", 15432, user, user, user);
        Object res1 = conn.query("select x from generate_series(1, 3) as x; select 42 as foo");
        System.out.println(res1);

        PreparedStatement ps = conn.prepare("select $1::int as foo");
        ArrayList<Object> params = new ArrayList<>();
        params.add(1);
        Object res2 = conn.executeStatement(ps, params);
        System.out.println(res2);

        // System.out.println(SourceType.STATEMENT.getCode());
    }
}
