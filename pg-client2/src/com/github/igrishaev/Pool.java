package com.github.igrishaev;

import java.util.*;

public class Pool {

    private final Map<UUID, Connection> connsUsed = new HashMap<>();
    private final Deque<Connection> connsFree = new ArrayDeque<>();

    private boolean isExpired (final Connection conn) {
        return conn.getCreatedAt() > 100500;
    }

    public synchronized Connection borrowConnection () {

        while (true) {
            final Connection conn = connsFree.poll();
            if (isExpired(conn)) {
                conn.close();
            }
            else {
                connsUsed.put(conn.getId(), conn);
                return conn;
            }

        }


    }

}
