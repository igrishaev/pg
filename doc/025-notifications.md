## Notifications

<!-- toc -->



<!-- tocstop -->

Notifications are somewhat pub-sub message system in Postgres. First you define
a channel. Then some actors produce messages to the channels and other actors
read them.

Imagine you have two connections: `conn1` and `conn2`. We use `conn1` to emit
messages into a channel and use `conn2` to listen that channel.

The `conn2` needs a handler function which is passed into the `:fn-notification`
configuration field. By default, it just prints the `NotificationResponse`
map. The better approach would be to process them in the background, say, using
futures, Manofold or core.async.

~~~clojure
(defn fn-notification [NotificationResponse]
  (future ;; or core.async
    (process-notification NotificationResponse)))

;; or

(def notifications!
  (atom []))

(defn fn-notification [NotificationResponse]
  (swap! notifications! conj NotificationResponse))

;; listener

(def conn2
  (pg/connect {:host "127.0.0.1"
               :port 5432
               ...
               :fn-notification fn-notification}))
~~~

The `fn-notification` function is processed in the connection's thread so it
would be improper to block it with time-heavy logic. Ideally, you put the
notification into some sort of a queue and let other parts of the system process
it.

To subscribe to a channel, run a LISTEN query. It takes a single parameter: a
name of the channel (without quotes).

~~~clojure
(pg/listen conn2 "FOO")
~~~

Now that you have a listening client, emit a couple of messages from the first
connection using NOTIFY:

~~~clojure
(def conn1 (pg/connect ...))

(pg/notify conn1 "FOO" "hello!")           ;; a string
(pg/notify conn1 "FOO" "[1, 2, 3]")        ;; JSON
(pg/notify conn1 "FOO" "{:color [a b c]}") ;; EDN
~~~

The NOTIFY expression accepts the name of the channel and a string message. At
the moment, there is no way to pass it as a parameter (but it's a subject to be
fixed).

**Attention: in Postgres, listening a channel is passive!** You won't receive a
message unless you perform a query to the server. It might be an empty query,
literally `SELECT` with nothing else; yet you've got to ping the server so it
sends all the collected messages to the client.

To have a listener that polls the server continuously, you need a background
task driven with `ScheduledExecutorService`, core.async, Manifold, or something
similar.

Here is the structure of the `NotificationResponse` map:

~~~clojure
{:msg :NotificationResponse
 :pid 123456
 :channel "FOO"
 :message "hello"}
~~~

The `:pid` is a number of process that has spawned this message. The PID might
be also known from the `(pg/pid conn)` call. You can have a map of PIDs for some
kind of routing or processing rules.

To stop listening a channel, run:

~~~clojure
(pg/unlisten conn2 "FOO")
~~~
