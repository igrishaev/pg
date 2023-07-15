;; Mostly machine-generated, see `fetch_oids.clj`
(ns pg.oid
  (:import
   clojure.lang.BigInt
   java.math.BigInteger
   java.math.BigDecimal
   java.util.Map
   java.util.HashMap
   java.util.UUID
   java.util.Date
   java.time.Instant)
  (:refer-clojure :exclude [char name time]))


(def ^int bool                             16)
(def ^int _bool                          1000)
(def ^int bytea                            17)
(def ^int _bytea                         1001)
(def ^int char                             18)
(def ^int _char                          1002)
(def ^int name                             19)
(def ^int _name                          1003)
(def ^int int8                             20)
(def ^int _int8                          1016)
(def ^int int2                             21)
(def ^int _int2                          1005)
(def ^int int2vector                       22)
(def ^int _int2vector                    1006)
(def ^int int4                             23)
(def ^int _int4                          1007)
(def ^int regproc                          24)
(def ^int _regproc                       1008)
(def ^int text                             25)
(def ^int _text                          1009)
(def ^int oid                              26)
(def ^int _oid                           1028)
(def ^int tid                              27)
(def ^int _tid                           1010)
(def ^int xid                              28)
(def ^int _xid                           1011)
(def ^int cid                              29)
(def ^int _cid                           1012)
(def ^int oidvector                        30)
(def ^int _oidvector                     1013)
(def ^int pg_type                          71)
(def ^int _pg_type                        210)
(def ^int pg_attribute                     75)
(def ^int _pg_attribute                   270)
(def ^int pg_proc                          81)
(def ^int _pg_proc                        272)
(def ^int pg_class                         83)
(def ^int _pg_class                       273)
(def ^int json                            114)
(def ^int _json                           199)
(def ^int xml                             142)
(def ^int _xml                            143)
(def ^int pg_node_tree                    194)
(def ^int pg_ndistinct                   3361)
(def ^int pg_dependencies                3402)
(def ^int pg_mcv_list                    5017)
(def ^int pg_ddl_command                   32)
(def ^int xid8                           5069)
(def ^int _xid8                           271)
(def ^int point                           600)
(def ^int _point                         1017)
(def ^int lseg                            601)
(def ^int _lseg                          1018)
(def ^int path                            602)
(def ^int _path                          1019)
(def ^int box                             603)
(def ^int _box                           1020)
(def ^int polygon                         604)
(def ^int _polygon                       1027)
(def ^int line                            628)
(def ^int _line                           629)
(def ^int float4                          700)
(def ^int _float4                        1021)
(def ^int float8                          701)
(def ^int _float8                        1022)
(def ^int unknown                         705)
(def ^int circle                          718)
(def ^int _circle                         719)
(def ^int money                           790)
(def ^int _money                          791)
(def ^int macaddr                         829)
(def ^int _macaddr                       1040)
(def ^int inet                            869)
(def ^int _inet                          1041)
(def ^int cidr                            650)
(def ^int _cidr                           651)
(def ^int macaddr8                        774)
(def ^int _macaddr8                       775)
(def ^int aclitem                        1033)
(def ^int _aclitem                       1034)
(def ^int bpchar                         1042)
(def ^int _bpchar                        1014)
(def ^int varchar                        1043)
(def ^int _varchar                       1015)
(def ^int date                           1082)
(def ^int _date                          1182)
(def ^int time                           1083)
(def ^int _time                          1183)
(def ^int timestamp                      1114)
(def ^int _timestamp                     1115)
(def ^int timestamptz                    1184)
(def ^int _timestamptz                   1185)
(def ^int interval                       1186)
(def ^int _interval                      1187)
(def ^int timetz                         1266)
(def ^int _timetz                        1270)
(def ^int bit                            1560)
(def ^int _bit                           1561)
(def ^int varbit                         1562)
(def ^int _varbit                        1563)
(def ^int numeric                        1700)
(def ^int _numeric                       1231)
(def ^int refcursor                      1790)
(def ^int _refcursor                     2201)
(def ^int regprocedure                   2202)
(def ^int _regprocedure                  2207)
(def ^int regoper                        2203)
(def ^int _regoper                       2208)
(def ^int regoperator                    2204)
(def ^int _regoperator                   2209)
(def ^int regclass                       2205)
(def ^int _regclass                      2210)
(def ^int regcollation                   4191)
(def ^int _regcollation                  4192)
(def ^int regtype                        2206)
(def ^int _regtype                       2211)
(def ^int regrole                        4096)
(def ^int _regrole                       4097)
(def ^int regnamespace                   4089)
(def ^int _regnamespace                  4090)
(def ^int uuid                           2950)
(def ^int _uuid                          2951)
(def ^int pg_lsn                         3220)
(def ^int _pg_lsn                        3221)
(def ^int tsvector                       3614)
(def ^int _tsvector                      3643)
(def ^int gtsvector                      3642)
(def ^int _gtsvector                     3644)
(def ^int tsquery                        3615)
(def ^int _tsquery                       3645)
(def ^int regconfig                      3734)
(def ^int _regconfig                     3735)
(def ^int regdictionary                  3769)
(def ^int _regdictionary                 3770)
(def ^int jsonb                          3802)
(def ^int _jsonb                         3807)
(def ^int jsonpath                       4072)
(def ^int _jsonpath                      4073)
(def ^int txid_snapshot                  2970)
(def ^int _txid_snapshot                 2949)
(def ^int pg_snapshot                    5038)
(def ^int _pg_snapshot                   5039)
(def ^int int4range                      3904)
(def ^int _int4range                     3905)
(def ^int numrange                       3906)
(def ^int _numrange                      3907)
(def ^int tsrange                        3908)
(def ^int _tsrange                       3909)
(def ^int tstzrange                      3910)
(def ^int _tstzrange                     3911)
(def ^int daterange                      3912)
(def ^int _daterange                     3913)
(def ^int int8range                      3926)
(def ^int _int8range                     3927)
(def ^int int4multirange                 4451)
(def ^int _int4multirange                6150)
(def ^int nummultirange                  4532)
(def ^int _nummultirange                 6151)
(def ^int tsmultirange                   4533)
(def ^int _tsmultirange                  6152)
(def ^int tstzmultirange                 4534)
(def ^int _tstzmultirange                6153)
(def ^int datemultirange                 4535)
(def ^int _datemultirange                6155)
(def ^int int8multirange                 4536)
(def ^int _int8multirange                6157)
(def ^int record                         2249)
(def ^int _record                        2287)
(def ^int cstring                        2275)
(def ^int _cstring                       1263)
(def ^int any                            2276)
(def ^int anyarray                       2277)
(def ^int void                           2278)
(def ^int trigger                        2279)
(def ^int event_trigger                  3838)
(def ^int language_handler               2280)
(def ^int internal                       2281)
(def ^int anyelement                     2283)
(def ^int anynonarray                    2776)
(def ^int anyenum                        3500)
(def ^int fdw_handler                    3115)
(def ^int index_am_handler                325)
(def ^int tsm_handler                    3310)
(def ^int table_am_handler                269)
(def ^int anyrange                       3831)
(def ^int anycompatible                  5077)
(def ^int anycompatiblearray             5078)
(def ^int anycompatiblenonarray          5079)
(def ^int anycompatiblerange             5080)
(def ^int anymultirange                  4537)
(def ^int anycompatiblemultirange        4538)
(def ^int pg_brin_bloom_summary          4600)
(def ^int pg_brin_minmax_multi_summary   4601)


(def ^:private oid-name->oid-int
  {"_int8multirange"              6157
   "json"                         114
   "pg_snapshot"                  5038
   "regconfig"                    3734
   "pg_type"                      71
   "_xid8"                        271
   "_box"                         1020
   "bytea"                        17
   "anycompatiblerange"           5080
   "numrange"                     3906
   "float8"                       701
   "tsrange"                      3908
   "numeric"                      1700
   "anyelement"                   2283
   "_cid"                         1012
   "internal"                     2281
   "_jsonb"                       3807
   "_xml"                         143
   "xml"                          142
   "cidr"                         650
   "_pg_lsn"                      3221
   "int8multirange"               4536
   "_regconfig"                   3735
   "void"                         2278
   "_name"                        1003
   "daterange"                    3912
   "bit"                          1560
   "_regproc"                     1008
   "_int4range"                   3905
   "anymultirange"                4537
   "macaddr"                      829
   "_tstzrange"                   3911
   "_tsrange"                     3909
   "_time"                        1183
   "tsquery"                      3615
   "_tsquery"                     3645
   "_uuid"                        2951
   "xid8"                         5069
   "_timetz"                      1270
   "record"                       2249
   "_float8"                      1022
   "_numrange"                    3907
   "int2vector"                   22
   "_interval"                    1187
   "_float4"                      1021
   "_nummultirange"               6151
   "pg_brin_bloom_summary"        4600
   "_polygon"                     1027
   "_aclitem"                     1034
   "regcollation"                 4191
   "language_handler"             2280
   "_regdictionary"               3770
   "pg_dependencies"              3402
   "_path"                        1019
   "regoperator"                  2204
   "path"                         602
   "anynonarray"                  2776
   "_tsmultirange"                6152
   "_txid_snapshot"               2949
   "anyarray"                     2277
   "jsonpath"                     4072
   "pg_ndistinct"                 3361
   "_int4multirange"              6150
   "nummultirange"                4532
   "timestamp"                    1114
   "jsonb"                        3802
   "_point"                       1017
   "int4range"                    3904
   "_regoper"                     2208
   "_regnamespace"                4090
   "_regoperator"                 2209
   "box"                          603
   "_oid"                         1028
   "float4"                       700
   "_numeric"                     1231
   "xid"                          28
   "_int2vector"                  1006
   "regdictionary"                3769
   "regnamespace"                 4089
   "_timestamp"                   1115
   "tsmultirange"                 4533
   "bpchar"                       1042
   "pg_lsn"                       3220
   "any"                          2276
   "tsm_handler"                  3310
   "_json"                        199
   "_regclass"                    2210
   "oid"                          26
   "name"                         19
   "tstzmultirange"               4534
   "oidvector"                    30
   "uuid"                         2950
   "int4"                         23
   "_inet"                        1041
   "interval"                     1186
   "inet"                         869
   "regoper"                      2203
   "_tid"                         1010
   "_bytea"                       1001
   "bool"                         16
   "char"                         18
   "fdw_handler"                  3115
   "varbit"                       1562
   "_text"                        1009
   "tsvector"                     3614
   "_regtype"                     2211
   "regprocedure"                 2202
   "datemultirange"               4535
   "_pg_attribute"                270
   "anycompatiblearray"           5078
   "text"                         25
   "_lseg"                        1018
   "anyrange"                     3831
   "_regprocedure"                2207
   "_regrole"                     4097
   "_bpchar"                      1014
   "txid_snapshot"                2970
   "time"                         1083
   "_gtsvector"                   3644
   "_regcollation"                4192
   "event_trigger"                3838
   "regrole"                      4096
   "anycompatiblenonarray"        5079
   "_datemultirange"              6155
   "anycompatible"                5077
   "_line"                        629
   "_int8"                        1016
   "line"                         628
   "aclitem"                      1033
   "gtsvector"                    3642
   "pg_node_tree"                 194
   "_pg_proc"                     272
   "int8"                         20
   "polygon"                      604
   "_varchar"                     1015
   "unknown"                      705
   "cstring"                      2275
   "regproc"                      24
   "anyenum"                      3500
   "_daterange"                   3913
   "int2"                         21
   "money"                        790
   "macaddr8"                     774
   "refcursor"                    1790
   "_macaddr8"                    775
   "_cidr"                        651
   "_bool"                        1000
   "date"                         1082
   "pg_brin_minmax_multi_summary" 4601
   "pg_class"                     83
   "_int2"                        1005
   "varchar"                      1043
   "pg_attribute"                 75
   "pg_proc"                      81
   "cid"                          29
   "_macaddr"                     1040
   "_date"                        1182
   "_bit"                         1561
   "anycompatiblemultirange"      4538
   "pg_mcv_list"                  5017
   "_int8range"                   3927
   "int8range"                    3926
   "_record"                      2287
   "_varbit"                      1563
   "int4multirange"               4451
   "index_am_handler"             325
   "trigger"                      2279
   "_refcursor"                   2201
   "circle"                       718
   "_cstring"                     1263
   "_jsonpath"                    4073
   "_char"                        1002
   "_circle"                      719
   "regclass"                     2205
   "tstzrange"                    3910
   "_pg_snapshot"                 5039
   "_pg_type"                     210
   "_timestamptz"                 1185
   "timetz"                       1266
   "timestamptz"                  1184
   "_money"                       791
   "point"                        600
   "tid"                          27
   "_tstzmultirange"              6153
   "_tsvector"                    3643
   "_pg_class"                    273
   "_int4"                        1007
   "regtype"                      2206
   "pg_ddl_command"               32
   "lseg"                         601
   "_oidvector"                   1013
   "_xid"                         1011
   "table_am_handler"             269})


(defn name->int ^Integer [^String oid-name]
  (get oid-name->oid-int oid-name))


(defn ->oid [x]
  (cond
    (int? x) x
    (string? x) (name->int x)))


(def ^:private ^Map -HINTS
  (doto (new HashMap)
    (.put Long       int8)
    (.put Integer    int4)
    (.put Short      int2)
    (.put Double     float8)
    (.put Float      float4)
    (.put String     text)
    (.put Boolean    bool)
    (.put UUID       uuid)
    (.put Date       timestamptz)
    (.put Instant    timestamptz)
    (.put BigDecimal numeric)
    (.put BigInteger numeric)
    (.put BigInt     numeric)))


(defn hint [value]
  (or (.get -HINTS (type value)) 0))


(defn add-hint [Type oid]
  (.put -HINTS Type oid))
