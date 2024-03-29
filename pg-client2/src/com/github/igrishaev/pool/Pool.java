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

public class Pool implements Closeable {

    private final ConnConfig connConfig;
    private final PoolConfig poolConfig;
    private final Map<UUID, Connection> connsUsed;
    private final Deque<Connection> connsFree;
    private boolean isClosed = false;
    private final static System.Logger.Level level = System.Logger.Level.INFO;
    private final static System.Logger logger = System.getLogger(Pool.class.getCanonicalName());

    public Pool (final ConnConfig connConfig) {
        this(connConfig, PoolConfig.standard());
    }

    public Pool (final ConnConfig connConfig, final PoolConfig poolConfig) {
        this.connConfig = connConfig;
        this.poolConfig = poolConfig;
        this.connsUsed = new HashMap<>(poolConfig.maxSize());
        this.connsFree = new ArrayDeque<>(poolConfig.maxSize());
        initiate();
    }

    private void initiate () {
        for (int i = 0; i < poolConfig.minSize(); i++) {
            final Connection conn = new Connection(connConfig);
            connsFree.add(conn);
        }
    }

    private boolean isExpired (final Connection conn) {
        return System.currentTimeMillis() - conn.getCreatedAt() > poolConfig.maxLifetime();
    }

    private void addUsed (final Connection conn) {
        connsUsed.put(conn.getId(), conn);
    }

    private void removeUsed (final Connection conn) {
        connsUsed.remove(conn.getId());
    }

    @SuppressWarnings("unused")
    private boolean isUsed (final Connection conn) {
        return connsUsed.containsKey(conn.getId());
    }

    @SuppressWarnings("unused")
    public synchronized Connection borrowConnection () {

        if (isClosed()) {
            throw new PGError("Cannot get a connection: the pool has been closed");
        }

        while (true) {
            final Connection conn = connsFree.poll();
            if (conn == null) {
                if (connsUsed.size() < poolConfig.maxSize()) {
                    return spawnConnection();
                }
                else {
                    final String message = String.format(
                            "The pool is exhausted: %s out of %s connections are in use",
                            connsUsed.size(),
                            poolConfig.maxSize()
                    );
                    logger.log(level, message);
                    throw new PGError(message);
                }
            }
            if (isExpired(conn)) {
                utilizeConnection(conn);
            }
            else {
                addUsed(conn);
                return conn;
            }
        }
    }

    @SuppressWarnings("unused")
    public synchronized void returnConnection (final Connection conn) {
        returnConnection(conn, false);
    }

    private void utilizeConnection(final Connection conn) {
        conn.close();
        logger.log(
                level,
                "the connection {0} has been closed, free: {1}, used: {2}, max: {3}",
                conn.getId(),
                connsFree.size(),
                connsUsed.size(),
                poolConfig.maxSize()
        );
    }

    private Connection spawnConnection() {
        final Connection conn = new Connection(connConfig);
        addUsed(conn);
        logger.log(
                level,
                "connection {0} has been created, free: {1}, used: {2}, max: {3}",
                conn.getId(),
                connsFree.size(),
                connsUsed.size(),
                poolConfig.maxSize()
        );
        return conn;
    }

    public synchronized void returnConnection (final Connection conn, final boolean forceClose) {

        if (!isUsed(conn)) {
            throw new PGError("connection %s doesn't belong to the pool", conn.getId());
        }

        removeUsed(conn);

        if (isClosed()) {
            utilizeConnection(conn);
            return;
        }

        if (conn.isClosed()) {
            return;
        }

        if (forceClose) {
            utilizeConnection(conn);
            return;
        }

        if (isExpired(conn)) {
            utilizeConnection(conn);
            return;
        }

        if (conn.isTxError()) {
            conn.rollback();
            utilizeConnection(conn);
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
        isClosed = true;
    }

    public synchronized boolean isClosed() {
        return isClosed;
    }

    @SuppressWarnings("unused")
    public synchronized int usedCount () {
        return connsUsed.size();
    }

    @SuppressWarnings("unused")
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
