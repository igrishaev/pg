package com.github.igrishaev;

public class Main {

    public static void main (String[] args) {
        Connection conn = new Connection("127.0.0.1", 15432, "ivan", "ivan", "ivan");
        System.out.println(conn.query("select x as x from generate_series(1, 9) as x"));
    }
}
