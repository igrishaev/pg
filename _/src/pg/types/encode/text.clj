(ns pg.types.encode.text
  (:import
   java.util.UUID
   ;; java.time.ZoneId
   ;; java.time.format.DateTimeFormatter
   ;; java.time.Instant
   )
  (:require
   [pg.const :as const]
   [pg.error :as e]
   [pg.codec :as codec]))


;;
;; Text
;;

(defmulti encode-text
  (fn [value enc]
    (class value)))


(defmethod encode-text :default
  [value enc]
  ::not-implemented)


(defmethod encode-text String
  [value enc]
  (codec/str->bytes value enc))


(defmethod encode-text Boolean
  [value enc]
  (codec/str->bytes (if value "t" "f") enc))


(defmethod encode-text Integer
  [value enc]
  (codec/str->bytes (str value) enc))


(defmethod encode-text Long
  [value enc]
  (codec/str->bytes (str value) enc))


(defmethod encode-text Float
  [value enc]
  (codec/str->bytes (str value) enc))


(defmethod encode-text Double
  [value enc]
  (codec/str->bytes (str value) enc))


(defmethod encode-text UUID
  [value enc]
  (codec/str->bytes (str value) enc))


;; (defn- ni? [x]
;;   (identical? x ::not-implemented))


;; (defn encode [value enc]
;;   (let [result
;;         (encode-binary value enc)]
;;     (if (ni? result)
;;       (let [result
;;             (encode-text value enc)]
;;         (if (ni? result)
;;           (e/error! "Cannot encode value"
;;                     {:value value})
;;           [const/FORMAT_TEXT result]))
;;       [const/FORMAT_BINARY result])))
