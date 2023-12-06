package com.github.igrishaev;

import java.util.ArrayList;
import java.util.Collections;

public class Main {

    public static void main (String[] args) {

        String user = System.getenv("USER");

        Config config = new Config.Builder(user, user)
                .port(15432)
                .host("127.0.0.1")
                .password(user)
                .binaryEncode(false)
                .binaryDecode(true)
                .build();


        // Connection conn = new Connection("127.0.0.1", 15432, user, user, user);
        Connection conn = new Connection(config);

        Object res1 = conn.query("select x from generate_series(1, 3) as x; select 42 as foo");
        System.out.println(res1);

        PreparedStatement ps = conn.prepare("select $1::int as foo");
        ArrayList<Object> params = new ArrayList<>();
        params.add(1);
        Object res2 = conn.executeStatement(ps, params);
        conn.closeStatement(ps);
        System.out.println(res2);

        Object res3 = conn.execute("select $1::int8 as int8", params, Collections.emptyList(), 999);
        System.out.println(res3);

        // System.out.println(SourceType.STATEMENT.getCode());
    }
}
