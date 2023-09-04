# Connection Pool

<!-- toc -->

- [Basic usage](#basic-usage)
- [Config](#config)
- [Thread safety](#thread-safety)
- [Exhausting](#exhausting)
- [Exceptions](#exceptions)
- [Logs](#logs)
- [Component](#component)

<!-- tocstop -->

To interact with a database effectively, you need a connection pool. A single
connection is a fragile thing on its own: you can easily lose it by an
accidental lag in the network.

Another thing which is worth bearing in mind is, opening a new connection every
time you want to reach PostgreSQL is expensive. Every connection starts a new
process on the server. If you have a cycle in your code that opens too many
connections or threads/futures that do the same, sooner or later you'll reach an
error response saying "too many connections".

The connection pool is an objects that holds several open connections at
once. It allows you to `borrow` a connection for some period of time. A borrowed
connection can be only used in a block of code that has borrowed it but nowhere
else. Once the block of code has done its duties, the connection gets returned
to the pool.

The pool is also capable of calculating the lifetime of connections and their
expiration moments. Once a connection has expired, it gets terminated and the
pool spawns a new connection.

The connection pool is shipped in a dedicated library as it depends on logging
facility.

## Basic usage

## Config

## Thread safety

## Exhausting

## Exceptions

## Logs

## Component
