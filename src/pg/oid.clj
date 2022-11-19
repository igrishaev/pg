(ns pg.oid
  "
  https://github.com/pgjdbc/pgjdbc/blob/0b097fd4a8e9990a9b86173d58633cd88d263b0b/pgjdbc/src/main/java/org/postgresql/core/Oid.java
  ")
                                   ;; dec enc
(def ^int UNSPECIFIED           0)
(def ^int INT2                 21) ;; 0 1
(def ^int INT2_ARRAY         1005)
(def ^int INT4                 23) ;; 0
(def ^int INT4_ARRAY         1007) ;; 0
(def ^int INT8                 20) ;; 0
(def ^int INT8_ARRAY         1016)
(def ^int TEXT                 25) ;; 0
(def ^int TEXT_ARRAY         1009)
(def ^int NUMERIC            1700) ;; 0
(def ^int NUMERIC_ARRAY      1231)
(def ^int FLOAT4              700) ;; 0
(def ^int FLOAT4_ARRAY       1021)
(def ^int FLOAT8              701) ;; 0
(def ^int FLOAT8_ARRAY       1022)
(def ^int BOOL                 16) ;; 0
(def ^int BOOL_ARRAY         1000)
(def ^int DATE               1082) ;; 0
(def ^int DATE_ARRAY         1182)
(def ^int TIME               1083) ;; 0
(def ^int TIME_ARRAY         1183)
(def ^int TIMETZ             1266)
(def ^int TIMETZ_ARRAY       1270)
(def ^int TIMESTAMP          1114)
(def ^int TIMESTAMP_ARRAY    1115)
(def ^int TIMESTAMPTZ        1184)
(def ^int TIMESTAMPTZ_ARRAY  1185)
(def ^int BYTEA                17) ;; 0
(def ^int BYTEA_ARRAY        1001)
(def ^int VARCHAR            1043) ;; 0
(def ^int VARCHAR_ARRAY      1015)
(def ^int OID                  26)
(def ^int OID_ARRAY          1028)
(def ^int BPCHAR             1042)
(def ^int BPCHAR_ARRAY       1014)
(def ^int MONEY               790)
(def ^int MONEY_ARRAY         791)
(def ^int NAME                 19)
(def ^int NAME_ARRAY         1003)
(def ^int BIT                1560)
(def ^int BIT_ARRAY          1561)
(def ^int VOID               2278)
(def ^int INTERVAL           1186)
(def ^int INTERVAL_ARRAY     1187)
(def ^int CHAR                 18)
(def ^int CHAR_ARRAY         1002)
(def ^int VARBIT             1562)
(def ^int VARBIT_ARRAY       1563)
(def ^int UUID               2950)
(def ^int UUID_ARRAY         2951)
(def ^int XML                 142)
(def ^int XML_ARRAY           143)
(def ^int POINT               600)
(def ^int POINT_ARRAY        1017)
(def ^int BOX                 603)
(def ^int JSONB              3802)
(def ^int JSONB_ARRAY        3807)
(def ^int JSON                114)
(def ^int JSON_ARRAY          199)
(def ^int REF_CURSOR         1790)
(def ^int REF_CURSOR_ARRAY   2201)
(def ^int LINE                628)
(def ^int LSEG                601)
(def ^int PATH                602)
(def ^int POLYGON             604)
(def ^int CIRCLE              718)
(def ^int CIDR                650)
(def ^int INET                869)
(def ^int MACADDR             829)
(def ^int MACADDR8            774)
(def ^int TSVECTOR           3614)
(def ^int TSQUERY            3615)

(def ^int INT2_VECTOR          22)
(def ^int INT2_VECTOR_ARRAY  1006)

;; 1042 | bpchar
;; 2249 | record
;; 2275 | cstring
;; 2276 | any
;; 2277 | anyarray
;; 4072 | jsonpath
