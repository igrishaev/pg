package com.github.igrishaev;

import java.util.ArrayList;

public class Main {

    public static void main (String[] args) {
        Connection conn = new Connection("127.0.0.1", 15432, "ivan", "ivan", "ivan");
        // Object result = conn.query("select x from generate_series(1, 3) as x; select 42 as foo");

        PreparedStatement ps = conn.prepare("select $1::int as foo");
        ArrayList<Object> params = new ArrayList<>();
        params.add(1);
        Object res = conn.executeStatement(ps, params);
        System.out.println(ps);

        // System.out.println(SourceType.STATEMENT.getCode());
    }
}