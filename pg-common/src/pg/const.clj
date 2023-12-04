(ns pg.const
  (:import
   java.time.Duration
   java.time.Instant
   java.time.LocalDate
   java.time.ZoneOffset))


(def ^Duration PG_EPOCH_DIFF
  (Duration/between Instant/EPOCH
                    (-> (LocalDate/of 2000 1 1)
                        (.atStartOfDay)
                        (.toInstant ZoneOffset/UTC))))


(def MICROS 1000000)

(def MILLIS 1000)

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
(def SSL_YES \S)
(def SSL_NO \N)

(def SCRAM_SHA_256 "SCRAM-SHA-256")

(def NUMERIC_POS (int 0x0000))
(def NUMERIC_NEG (int 0x4000))
(def NUMERIC_NAN (int 0xC000))

(def COPY_BUFFER_SIZE 2048)

(def COPY_FAIL_MSG "COPY has been interrupted by the client")

(def COPY_CSV_CELL_SEP \,)
(def COPY_CSV_LINE_SEP "\r\n")
(def COPY_CSV_NULL "")

(def COPY_FORMAT_TXT :txt)
(def COPY_FORMAT_CSV :csv)
(def COPY_FORMAT_BIN :bin)

(def IN_STREAM_BUF_SIZE 0xFFFF)
