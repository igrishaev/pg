(ns pg.handler
  )


(defn notice-handler
  [conn messages]
  (println "Server notice:")
  (doseq [{:keys [type message]} messages]
    (println " -" type message)))


(defn notification-handler
  [conn pid ^String channel ^String message]
  (println
   (format "Server notification: PID %s, channel: %s, message: %s"
           pid channel (or message "<empty>"))))
