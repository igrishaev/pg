package com.github.igrishaev.pool;

import com.github.igrishaev.ConnConfig;
import com.github.igrishaev.Connection;
import com.github.igrishaev.PGError;

import java.io.Closeable;
import java.util.Deque;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

public record Pool (
        ConnConfig connConfig,
        PoolConfig poolConfig,
        Map<UUID, Connection> connsUsed,
        Deque<Connection> connsFree

) implements Closeable {

    public static Pool of(final ConnConfig connConfig) {
        return Pool.of(connConfig, PoolConfig.standard());
    }

    public static Pool of(final ConnConfig connConfig, final PoolConfig poolConfig) {
        final Pool pool = new Pool(
                connConfig,
                poolConfig,
                new HashMap<>(poolConfig.maxSize()),
                new ArrayDeque<>(poolConfig.maxSize())
        );
        pool.initiate();
        return pool;
    }

    private void initiate () {
        for (int i = 0; i < poolConfig().minSize(); i++) {
            final Connection conn = new Connection(connConfig);
            connsFree.add(conn);
        }
    }

    private boolean isExpired (final Connection conn) {
        return System.currentTimeMillis() - conn.getCreatedAt() > poolConfig().maxLifetime();
    }

    private void addUsed (final Connection conn) {
        connsUsed.put(conn.getId(), conn);
    }

    private void removeUsed (final Connection conn) {
        connsUsed.remove(conn.getId());
    }

    private boolean isUsed (final Connection conn) {
        return connsUsed.containsKey(conn.getId());
    }

    public synchronized Connection borrowConnection () {
        while (true) {
            final Connection conn = connsFree.poll();
            if (conn == null) {
                if (connsUsed.size() < poolConfig().maxSize()) {
                    final Connection connNew = new Connection(connConfig);
                    addUsed(connNew);
                    return connNew;
                }
                else {
                    throw new PGError(
                            "The pool is exhausted: %s connections are in use",
                            poolConfig().maxSize()
                    );
                }
            }
            if (isExpired(conn)) {
                conn.close();
            }
            else {
                addUsed(conn);
                return conn;
            }
        }
    }

    public synchronized void returnConnection (final Connection conn) {
        returnConnection(conn, false);
    }

    public synchronized void returnConnection (final Connection conn, final boolean forceClose) {

        if (!isUsed(conn)) {
            throw new PGError("connection %s doesn't belong to the pool", conn.getId());
        }

        removeUsed(conn);

        if (conn.isClosed()) {
            return;
        }

        if (forceClose) {
            conn.close();
            return;
        }

        if (isExpired(conn)) {
            conn.close();
            return;
        }

        if (conn.isTxError()) {
            conn.rollback();
            conn.close();
            return;
        }

        if (conn.isTransaction()) {
            conn.rollback();
        }

        connsFree.offer(conn);
    }

    public synchronized void close () {
        for (final Connection conn: connsFree) {
            conn.close();
        }
        for (final Connection conn: connsUsed.values()) {
            conn.close();
        }
    }

    public synchronized int usedCount () {
        return connsUsed.size();
    }

    public synchronized int freeCount () {
        return connsFree.size();
    }

    public synchronized String toString () {
        return String.format(
                "<PG pool, min: %s, max: %s, lifetime: %s>",
                poolConfig.minSize(),
                poolConfig.maxSize(),
                poolConfig.maxLifetime()
        );
    }
}
