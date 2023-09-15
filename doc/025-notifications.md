## Notifications

<!-- toc -->

- [Introduction](#introduction)
- [Usage](#usage)

<!-- tocstop -->

### Introduction

Notifications are somewhat pub-sub message systems in Postgres. They can be
described in these simple steps:

- client B subscribes to a channel; the channel gets created if it doesn't
  exist.

- client A sends a message to that channel;

- every time client B interacts with the database, they receive messages sent to
  this channel by other clients.

To handle a message, the client invokes a special handler. This handler comes
from the configuration. The default handler just prints the notification
map. **Pay attention** that the handler is called synchronously blocking the
interaction with a socket. To prevent the connection from hanging due to the
time-consuming handling of a notification, provide a handler that sends it to
some sort of channel, agent, or message queue system.

### Usage

Imagine you have two connections: `conn1` and `conn2`:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})

(def conn1
  (pg/connect config))

(def conn2
  (pg/connect config))
~~~

Let `conn2` listen for a random channel:

~~~clojure
(def channel "hello")

(pg/listen conn2 channel)
~~~

Let the `conn1` connection send something into that channel:

~~~clojure
(pg/notify conn1 channel "test")
~~~

Now, should `conn2` interact with the database, it will handle the notification
by printing it to the console:

~~~clojure
(pg/query conn2 "select")

;; PG notification: {:msg :NotificationResponse, :pid 11244, :channel "hello", :message "test"}
~~~

A notification is a map which tracks the channel name, the number of the process
number (PID) of the connection that has emitted it and the string payload of the
notification.

To override the default printing handler, first declare a function. In our
example, the function just stores the notifications in a global atom:

~~~clojure
(def notifications
  (atom []))

(defn my-handler [notification]
  (swap! notifications conj notification))
~~~

Let's go through the pipeline again with a new handler. Open a connection with
the new hanlder and subscribe to the channel:

~~~clojure
(def conn2
  (pg/connect (assoc config :fn-notification my-handler)))

(pg/listen conn2 channel)
~~~

Send a couple of messages from another connection:

~~~clojure
(pg/notify conn1 channel "test1")
(pg/notify conn1 channel "test2")
~~~

Trigger receiving the messages and check out the atom:

~~~clojure
(pg/query conn2 "select")

@notifications

[{:msg :NotificationResponse, :pid 11244, :channel "hello", :message "test1"}
 {:msg :NotificationResponse, :pid 11244, :channel "hello", :message "test2"}]
~~~

Futures, thread executors or core.async/Manofold are your best friends to
organize background processing of notifications effectively.

Keep in mind that the listenting connection is passive: you won't get any of
pending messages unless you interact with the database somehow. Running an empty
query from time to time would solve the problem. Again, you may have a backgound
loop or a scheduled task that does it for you.

To stop receiving notifications from a certain channel, call `unlisten`:

~~~clojure
(pg/unlisten conn2 channel)
~~~
