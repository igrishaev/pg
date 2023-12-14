package com.github.igrishaev;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;

public class Main {

    public static void main (String[] args) {

        String user = System.getenv("USER");

//        Config config = new Config.Builder(user, user)
//                .port(15432)
//                .host("127.0.0.1")
//                .password(user)
//                .binaryEncode(true)
//                .binaryDecode(true)
//                .build();

        Config config = new Config.Builder("test", "test")
                .port(10130)
                .host("127.0.0.1")
                .password("test")
                .binaryEncode(true)
                .binaryDecode(true)
                .build();

        // Connection conn = new Connection("127.0.0.1", 15432, user, user, user);
        Connection conn = new Connection(config);

        System.out.println(conn.getId());
        System.out.println(conn.getPid());

        System.out.println(conn.execute("select '1 year 1 second'::interval as interval"));

        // Object res1 = conn.query("select x from generate_series(1, 3) as x; select 42 as foo");
        // System.out.println(res1);

        // System.out.println(conn.execute(""));

//        PreparedStatement ps = conn.prepare("select $1::int as foo");
//        ArrayList<Object> params = new ArrayList<>();
//        params.add(1);
//        Object res2 = conn.executeStatement(ps, params);
//        conn.closeStatement(ps);
//        System.out.println(res2);

        // Object res3 = conn.execute("select $1::int8 as int8", params, Collections.emptyList());
        // System.out.println(res3);

        // conn.execute("create table abc (id integer, title text)");
//        Object resIns = conn.execute(
//                "insert into abc (id, title) values ($1, $2), ($3, $4) returning *",
//                new ExecuteParams.Builder().params(1, "test2", 2, "test2").build()
//        );
//
//        System.out.println(resIns);
//
//        Object res4 = conn.query("copy (select s.x as x, s.x * s.x as square from generate_series(1, 9) as s(x)) TO STDOUT WITH (FORMAT CSV)");
//        System.out.println(res4);
//
//        FileOutputStream out;
//        try {
//            out = new FileOutputStream("foo.csv");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        Object res5 = conn.copyOut("copy (select s.x as x, s.x * s.x as square from generate_series(1, 9) as s(x)) TO STDOUT WITH (FORMAT CSV)", out);
//        System.out.println(res5);
//
//        Object res6 = conn.query("select '[1, 2, 3, {\"foo/bar\": true}]'::jsonb");
//        System.out.println(res6);
//
//        Object res7 = conn.execute("select '[1, 2, 3, {\"foo/bar\": true}]'::jsonb");
//        System.out.println(res7);



        // System.out.println(SourceType.STATEMENT.getCode());
    }
}
