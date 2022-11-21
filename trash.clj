

;; https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/jdbc/TimestampUtils.java

(Float/intBitsToFloat (new BigInteger (byte-array [63, -116, -52, -51])))

(Double/longBitsToDouble (new BigInteger (byte-array [63, -15, -103, -103, -103, -103, -103, -102])))


  ;; ArrayList
  ;; Map
  ;; UUID

  ;; Timestamp
  ;; Time
  ;; Date

  ;; IPersistentCollection

  ;; Symbol
  ;; Keyword

  ;; nil

  ;; Point
  ;; Box
  ;; Line
  ;; LineSegment
  ;; Circle
  ;; Polygon

  ;; bytes

  ;; Inet
  ;; Cidr

  ;; Char
  ;; Long
  ;; Float
  ;; Double
  ;; BigDecimal
  ;; Number

(defn bytes->int32 [^bytes buf]
  (let [b4 (aget buf 0)
        b3 (aget buf 1)
        b2 (aget buf 2)
        b1 (aget buf 3)]
    (+ (bit-shift-left b4 24)
       (bit-shift-left b3 16)
       (bit-shift-left b2 8)
       b1)))

#_
(defn bytes->int [^bytes buf]
  (let [len (alength buf)
        res 0]
    (loop [i 0]
      (if (= i len)
        res
        (+ res (bit-shift-left (aget buf i) (- len i 1)))))))


(defn int32->bytes ^bytes [int32]
  (let [buf (byte-array 4)

        b4 (-> int32 (bit-and 0xff000000) (bit-shift-right 24) unchecked-byte)
        b3 (-> int32 (bit-and 0x00ff0000) (bit-shift-right 16) unchecked-byte)
        b2 (-> int32 (bit-and 0x0000ff00) (bit-shift-right 8) unchecked-byte)
        b1 (-> int32 (bit-and 0x000000ff) unchecked-byte)]

    (aset buf 0 b4)
    (aset buf 1 b3)
    (aset buf 2 b2)
    (aset buf 3 b1)

    buf))


(defn read-cstring [^InputStream in]

  (let [o (new ByteArrayOutputStream)]
    (loop []
      (let [b (.read in)]
        (cond
          (= -1 b)
          nil

          (= 0 b)
          (.toString o "UTF-8")

          :else
          (do
            (.write o b)
            (recur))))))

  #_
  (let [s (new Scanner in "UTF-8")]
    (.useDelimiter s "\0")
    (when (.hasNext s)
      (.next s)))

  #_
  (with-open [r (new InputStreamReader in "UTF-8")]
    (with-open [b (new BufferedReader r 1)]
      (loop [sb (new StringBuilder)]
        (let [c (.read b)]
          (cond
            (= -1 c)
            :EOF

            (= 0 c)
            (str sb)

            :else
            (recur (.append sb (char c)))))))))

select
1 as foo,
'hello' as bar,
true as aaa,
1.1::float as fl,
1.1::float4 as fl1,
1.1::float8 as fl2,
1.2 as fl3,
NULL as nil,
now() as date,
'{1, 2, 3}'::int2vector[] as intvec

-- '{1, 2, 3}'::int2[] as arr1


(-> (query -conn "select '{\"a''aa\\\"a\",\"bb,bb\",aaaa,ываыа}'::text[] as one") first first second println)

(-> (query -conn "select array[$1,$2] as val" ["a\ta" "ccc\na\r\naa"] [pg.oid/TEXT pg.oid/TEXT] const/FORMAT_BINARY) first first second println)


(-> (query -conn "select gen_random_uuid() as uuid" [] [] const/FORMAT_BINARY))

(defmethod print-method (type (byte-array []))
  [b writer]
  (print-method (vec b) writer))

(query -conn "select TIMESTAMP WITH TIME ZONE '2004-10-19 10:23:54+02'")

"\\x6162635c6e616161"
(char (Integer/parseInt (str (char 54) (char 49)) 16))
[92, 120, 54, 49, 54, 50, 54, 51, 53, 99, 54, 101, 54, 49, 54, 49, 54, 49]

(query -conn "select INTERVAL '1 years 3 day 1 hour 1 sec'")


(query -conn "select 'aaaa'::void as void")
{:type :RowDescription, :field-count 1, :fields [{:index 0, :name [118 111 105 100], :table-id 0, :column-id 0, :type-id 2278, :type-len 4, :type-mod -1, :format 0}]}
{:type :DataRow, :columns [[]]}
{:type :CommandComplete, :tag SELECT 1}
{:type :ReadyForQuery, :tx-status I}
[{:void ""}]


(query -conn "select array['sd\\r,sf','sd,fs''sf','sdfs\"fdsfd','sdf sdf'] as arr")
{:type :RowDescription, :field-count 1, :fields [{:index 0, :name [97 114 114], :table-id 0, :column-id 0, :type-id 1009, :type-len -1, :type-mod -1, :format 0}]}
{:type :DataRow, :columns [[123 34 115 100 92 92 114 44 115 102 34 44 34 115 100 44 102 115 39 115 102 34 44 34 115 100 102 115 92 34 102 100 115 102 100 34 44 34 115 100 102 32 115 100 102 34 125]]}
{:type :CommandComplete, :tag SELECT 1}
{:type :ReadyForQuery, :tx-status I}
[{:arr "{\"sd\\\\r,sf\",\"sd,fs'sf\",\"sdfs\\\"fdsfd\",\"sdf sdf\"}"}]


https://stackoverflow.com/questions/24408984/convert-bytearray-to-uuid-java

> (-> (query -conn "select gen_random_uuid() as uuid" [] [] const/FORMAT_BINARY))
[{:uuid [107, 85, 96, 117, 66, 16, 65, -76, -74, 9, -33, -14, -71, 30, 8, -19]}]
pg.api
> (count [107, 85, 96, 117, 66, 16, 65, -76, -74, 9, -33, -14, -71, 30, 8, -19])
16
