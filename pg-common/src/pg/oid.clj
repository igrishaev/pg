;; Mostly machine-generated, see `fetch_oids.clj`
(ns pg.oid
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


(def ^:private -name->oid {
  "_int8multirange"             _int8multirange
  "json"                        json
  "pg_snapshot"                 pg_snapshot
  "regconfig"                   regconfig
  "pg_type"                     pg_type
  "_xid8"                       _xid8
  "_box"                        _box
  "bytea"                       bytea
  "anycompatiblerange"          anycompatiblerange
  "numrange"                    numrange
  "float8"                      float8
  "tsrange"                     tsrange
  "numeric"                     numeric
  "anyelement"                  anyelement
  "_cid"                        _cid
  "internal"                    internal
  "_jsonb"                      _jsonb
  "_xml"                        _xml
  "xml"                         xml
  "cidr"                        cidr
  "_pg_lsn"                     _pg_lsn
  "int8multirange"              int8multirange
  "_regconfig"                  _regconfig
  "void"                        void
  "_name"                       _name
  "daterange"                   daterange
  "bit"                         bit
  "_regproc"                    _regproc
  "_int4range"                  _int4range
  "anymultirange"               anymultirange
  "macaddr"                     macaddr
  "_tstzrange"                  _tstzrange
  "_tsrange"                    _tsrange
  "_time"                       _time
  "tsquery"                     tsquery
  "_tsquery"                    _tsquery
  "_uuid"                       _uuid
  "xid8"                        xid8
  "_timetz"                     _timetz
  "record"                      record
  "_float8"                     _float8
  "_numrange"                   _numrange
  "int2vector"                  int2vector
  "_interval"                   _interval
  "_float4"                     _float4
  "_nummultirange"              _nummultirange
  "pg_brin_bloom_summary"       pg_brin_bloom_summary
  "_polygon"                    _polygon
  "_aclitem"                    _aclitem
  "regcollation"                regcollation
  "language_handler"            language_handler
  "_regdictionary"              _regdictionary
  "pg_dependencies"             pg_dependencies
  "_path"                       _path
  "regoperator"                 regoperator
  "path"                        path
  "anynonarray"                 anynonarray
  "_tsmultirange"               _tsmultirange
  "_txid_snapshot"              _txid_snapshot
  "anyarray"                    anyarray
  "jsonpath"                    jsonpath
  "pg_ndistinct"                pg_ndistinct
  "_int4multirange"             _int4multirange
  "nummultirange"               nummultirange
  "timestamp"                   timestamp
  "jsonb"                       jsonb
  "_point"                      _point
  "int4range"                   int4range
  "_regoper"                    _regoper
  "_regnamespace"               _regnamespace
  "_regoperator"                _regoperator
  "box"                         box
  "_oid"                        _oid
  "float4"                      float4
  "_numeric"                    _numeric
  "xid"                         xid
  "_int2vector"                 _int2vector
  "regdictionary"               regdictionary
  "regnamespace"                regnamespace
  "_timestamp"                  _timestamp
  "tsmultirange"                tsmultirange
  "bpchar"                      bpchar
  "pg_lsn"                      pg_lsn
  "any"                         any
  "tsm_handler"                 tsm_handler
  "_json"                       _json
  "_regclass"                   _regclass
  "oid"                         oid
  "name"                        name
  "tstzmultirange"              tstzmultirange
  "oidvector"                   oidvector
  "uuid"                        uuid
  "int4"                        int4
  "_inet"                       _inet
  "interval"                    interval
  "inet"                        inet
  "regoper"                     regoper
  "_tid"                        _tid
  "_bytea"                      _bytea
  "bool"                        bool
  "char"                        char
  "fdw_handler"                 fdw_handler
  "varbit"                      varbit
  "_text"                       _text
  "tsvector"                    tsvector
  "_regtype"                    _regtype
  "regprocedure"                regprocedure
  "datemultirange"              datemultirange
  "_pg_attribute"               _pg_attribute
  "anycompatiblearray"          anycompatiblearray
  "text"                        text
  "_lseg"                       _lseg
  "anyrange"                    anyrange
  "_regprocedure"               _regprocedure
  "_regrole"                    _regrole
  "_bpchar"                     _bpchar
  "txid_snapshot"               txid_snapshot
  "time"                        time
  "_gtsvector"                  _gtsvector
  "_regcollation"               _regcollation
  "event_trigger"               event_trigger
  "regrole"                     regrole
  "anycompatiblenonarray"       anycompatiblenonarray
  "_datemultirange"             _datemultirange
  "anycompatible"               anycompatible
  "_line"                       _line
  "_int8"                       _int8
  "line"                        line
  "aclitem"                     aclitem
  "gtsvector"                   gtsvector
  "pg_node_tree"                pg_node_tree
  "_pg_proc"                    _pg_proc
  "int8"                        int8
  "polygon"                     polygon
  "_varchar"                    _varchar
  "unknown"                     unknown
  "cstring"                     cstring
  "regproc"                     regproc
  "anyenum"                     anyenum
  "_daterange"                  _daterange
  "int2"                        int2
  "money"                       money
  "macaddr8"                    macaddr8
  "refcursor"                   refcursor
  "_macaddr8"                   _macaddr8
  "_cidr"                       _cidr
  "_bool"                       _bool
  "date"                        date
  "pg_brin_minmax_multi_summary" pg_brin_minmax_multi_summary
  "pg_class"                    pg_class
  "_int2"                       _int2
  "varchar"                     varchar
  "pg_attribute"                pg_attribute
  "pg_proc"                     pg_proc
  "cid"                         cid
  "_macaddr"                    _macaddr
  "_date"                       _date
  "_bit"                        _bit
  "anycompatiblemultirange"     anycompatiblemultirange
  "pg_mcv_list"                 pg_mcv_list
  "_int8range"                  _int8range
  "int8range"                   int8range
  "_record"                     _record
  "_varbit"                     _varbit
  "int4multirange"              int4multirange
  "index_am_handler"            index_am_handler
  "trigger"                     trigger
  "_refcursor"                  _refcursor
  "circle"                      circle
  "_cstring"                    _cstring
  "_jsonpath"                   _jsonpath
  "_char"                       _char
  "_circle"                     _circle
  "regclass"                    regclass
  "tstzrange"                   tstzrange
  "_pg_snapshot"                _pg_snapshot
  "_pg_type"                    _pg_type
  "_timestamptz"                _timestamptz
  "timetz"                      timetz
  "timestamptz"                 timestamptz
  "_money"                      _money
  "point"                       point
  "tid"                         tid
  "_tstzmultirange"             _tstzmultirange
  "_tsvector"                   _tsvector
  "_pg_class"                   _pg_class
  "_int4"                       _int4
  "regtype"                     regtype
  "pg_ddl_command"              pg_ddl_command
  "lseg"                        lseg
  "_oidvector"                  _oidvector
  "_xid"                        _xid
  "table_am_handler"            table_am_handler
})

(def array-oids #{
  _bool
  _bytea
  _char
  _name
  _int8
  _int2
  _int2vector
  _int4
  _regproc
  _text
  _oid
  _tid
  _xid
  _cid
  _oidvector
  _pg_type
  _pg_attribute
  _pg_proc
  _pg_class
  _json
  _xml
  _xid8
  _point
  _lseg
  _path
  _box
  _polygon
  _line
  _float4
  _float8
  _circle
  _money
  _macaddr
  _inet
  _cidr
  _macaddr8
  _aclitem
  _bpchar
  _varchar
  _date
  _time
  _timestamp
  _timestamptz
  _interval
  _timetz
  _bit
  _varbit
  _numeric
  _refcursor
  _regprocedure
  _regoper
  _regoperator
  _regclass
  _regcollation
  _regtype
  _regrole
  _regnamespace
  _uuid
  _pg_lsn
  _tsvector
  _gtsvector
  _tsquery
  _regconfig
  _regdictionary
  _jsonb
  _jsonpath
  _txid_snapshot
  _pg_snapshot
  _int4range
  _numrange
  _tsrange
  _tstzrange
  _daterange
  _int8range
  _int4multirange
  _nummultirange
  _tsmultirange
  _tstzmultirange
  _datemultirange
  _int8multirange
  _cstring
})


(defn name->oid [^String oid-name]
  (get -name->oid oid-name))


(defn ->oid [x]
  (cond
    (int? x) x
    (string? x) (name->oid x)))
