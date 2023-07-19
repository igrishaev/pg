(ns pg.client.const)

;; TODO: const

(def PROTOCOL_VERSION 196608)

(def SOURCE_PORTAL \P)
(def SOURCE_STATEMENT \S)

(def FORMAT_TXT 0)
(def FORMAT_BIN 1)

(def CANCEL_CODE 80877102)

(def TX_ERROR :E)
(def TX_TRANSACTION :T)
(def TX_IDLE :I)

(def SSL_CODE 80877103)

(def SCRAM_SHA_256 "SCRAM-SHA-256")
