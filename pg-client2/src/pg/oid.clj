(ns pg.oid
  (:import
   com.github.igrishaev.enums.OID))

(def ^OID default OID/DEFAULT)
(def ^OID bool OID/BOOL)
(def ^OID _bool OID/_BOOL)
(def ^OID bytea OID/BYTEA)
(def ^OID _bytea OID/_BYTEA)
(def ^OID char OID/CHAR)
(def ^OID _char OID/_CHAR)
(def ^OID name OID/NAME)
(def ^OID _name OID/_NAME)
(def ^OID int8 OID/INT8)
(def ^OID _int8 OID/_INT8)
(def ^OID int2 OID/INT2)
(def ^OID _int2 OID/_INT2)
(def ^OID int2vector OID/INT2VECTOR)
(def ^OID _int2vector OID/_INT2VECTOR)
(def ^OID int4 OID/INT4)
(def ^OID _int4 OID/_INT4)
(def ^OID regproc OID/REGPROC)
(def ^OID _regproc OID/_REGPROC)
(def ^OID text OID/TEXT)
(def ^OID _text OID/_TEXT)
(def ^OID oid OID/OID)
(def ^OID _oid OID/_OID)
(def ^OID tid OID/TID)
(def ^OID _tid OID/_TID)
(def ^OID xid OID/XID)
(def ^OID _xid OID/_XID)
(def ^OID cid OID/CID)
(def ^OID _cid OID/_CID)
(def ^OID oidvector OID/OIDVECTOR)
(def ^OID _oidvector OID/_OIDVECTOR)
(def ^OID pg_type OID/PG_TYPE)
(def ^OID _pg_type OID/_PG_TYPE)
(def ^OID pg_attribute OID/PG_ATTRIBUTE)
(def ^OID _pg_attribute OID/_PG_ATTRIBUTE)
(def ^OID pg_proc OID/PG_PROC)
(def ^OID _pg_proc OID/_PG_PROC)
(def ^OID pg_class OID/PG_CLASS)
(def ^OID _pg_class OID/_PG_CLASS)
(def ^OID json OID/JSON)
(def ^OID _json OID/_JSON)
(def ^OID xml OID/XML)
(def ^OID _xml OID/_XML)
(def ^OID pg_node_tree OID/PG_NODE_TREE)
(def ^OID pg_ndistinct OID/PG_NDISTINCT)
(def ^OID pg_dependencies OID/PG_DEPENDENCIES)
(def ^OID pg_mcv_list OID/PG_MCV_LIST)
(def ^OID pg_ddl_command OID/PG_DDL_COMMAND)
(def ^OID xid8 OID/XID8)
(def ^OID _xid8 OID/_XID8)
(def ^OID point OID/POINT)
(def ^OID _point OID/_POINT)
(def ^OID lseg OID/LSEG)
(def ^OID _lseg OID/_LSEG)
(def ^OID path OID/PATH)
(def ^OID _path OID/_PATH)
(def ^OID box OID/BOX)
(def ^OID _box OID/_BOX)
(def ^OID polygon OID/POLYGON)
(def ^OID _polygon OID/_POLYGON)
(def ^OID line OID/LINE)
(def ^OID _line OID/_LINE)
(def ^OID float4 OID/FLOAT4)
(def ^OID _float4 OID/_FLOAT4)
(def ^OID float8 OID/FLOAT8)
(def ^OID _float8 OID/_FLOAT8)
(def ^OID unknown OID/UNKNOWN)
(def ^OID circle OID/CIRCLE)
(def ^OID _circle OID/_CIRCLE)
(def ^OID money OID/MONEY)
(def ^OID _money OID/_MONEY)
(def ^OID macaddr OID/MACADDR)
(def ^OID _macaddr OID/_MACADDR)
(def ^OID inet OID/INET)
(def ^OID _inet OID/_INET)
(def ^OID cidr OID/CIDR)
(def ^OID _cidr OID/_CIDR)
(def ^OID macaddr8 OID/MACADDR8)
(def ^OID _macaddr8 OID/_MACADDR8)
(def ^OID aclitem OID/ACLITEM)
(def ^OID _aclitem OID/_ACLITEM)
(def ^OID bpchar OID/BPCHAR)
(def ^OID _bpchar OID/_BPCHAR)
(def ^OID varchar OID/VARCHAR)
(def ^OID _varchar OID/_VARCHAR)
(def ^OID date OID/DATE)
(def ^OID _date OID/_DATE)
(def ^OID time OID/TIME)
(def ^OID _time OID/_TIME)
(def ^OID timestamp OID/TIMESTAMP)
(def ^OID _timestamp OID/_TIMESTAMP)
(def ^OID timestamptz OID/TIMESTAMPTZ)
(def ^OID _timestamptz OID/_TIMESTAMPTZ)
(def ^OID interval OID/INTERVAL)
(def ^OID _interval OID/_INTERVAL)
(def ^OID timetz OID/TIMETZ)
(def ^OID _timetz OID/_TIMETZ)
(def ^OID bit OID/BIT)
(def ^OID _bit OID/_BIT)
(def ^OID varbit OID/VARBIT)
(def ^OID _varbit OID/_VARBIT)
(def ^OID numeric OID/NUMERIC)
(def ^OID _numeric OID/_NUMERIC)
(def ^OID refcursor OID/REFCURSOR)
(def ^OID _refcursor OID/_REFCURSOR)
(def ^OID regprocedure OID/REGPROCEDURE)
(def ^OID _regprocedure OID/_REGPROCEDURE)
(def ^OID regoper OID/REGOPER)
(def ^OID _regoper OID/_REGOPER)
(def ^OID regoperator OID/REGOPERATOR)
(def ^OID _regoperator OID/_REGOPERATOR)
(def ^OID regclass OID/REGCLASS)
(def ^OID _regclass OID/_REGCLASS)
(def ^OID regcollation OID/REGCOLLATION)
(def ^OID _regcollation OID/_REGCOLLATION)
(def ^OID regtype OID/REGTYPE)
(def ^OID _regtype OID/_REGTYPE)
(def ^OID regrole OID/REGROLE)
(def ^OID _regrole OID/_REGROLE)
(def ^OID regnamespace OID/REGNAMESPACE)
(def ^OID _regnamespace OID/_REGNAMESPACE)
(def ^OID uuid OID/UUID)
(def ^OID _uuid OID/_UUID)
(def ^OID pg_lsn OID/PG_LSN)
(def ^OID _pg_lsn OID/_PG_LSN)
(def ^OID tsvector OID/TSVECTOR)
(def ^OID _tsvector OID/_TSVECTOR)
(def ^OID gtsvector OID/GTSVECTOR)
(def ^OID _gtsvector OID/_GTSVECTOR)
(def ^OID tsquery OID/TSQUERY)
(def ^OID _tsquery OID/_TSQUERY)
(def ^OID regconfig OID/REGCONFIG)
(def ^OID _regconfig OID/_REGCONFIG)
(def ^OID regdictionary OID/REGDICTIONARY)
(def ^OID _regdictionary OID/_REGDICTIONARY)
(def ^OID jsonb OID/JSONB)
(def ^OID _jsonb OID/_JSONB)
(def ^OID jsonpath OID/JSONPATH)
(def ^OID _jsonpath OID/_JSONPATH)
(def ^OID txid_snapshot OID/TXID_SNAPSHOT)
(def ^OID _txid_snapshot OID/_TXID_SNAPSHOT)
(def ^OID pg_snapshot OID/PG_SNAPSHOT)
(def ^OID _pg_snapshot OID/_PG_SNAPSHOT)
(def ^OID int4range OID/INT4RANGE)
(def ^OID _int4range OID/_INT4RANGE)
(def ^OID numrange OID/NUMRANGE)
(def ^OID _numrange OID/_NUMRANGE)
(def ^OID tsrange OID/TSRANGE)
(def ^OID _tsrange OID/_TSRANGE)
(def ^OID tstzrange OID/TSTZRANGE)
(def ^OID _tstzrange OID/_TSTZRANGE)
(def ^OID daterange OID/DATERANGE)
(def ^OID _daterange OID/_DATERANGE)
(def ^OID int8range OID/INT8RANGE)
(def ^OID _int8range OID/_INT8RANGE)
(def ^OID int4multirange OID/INT4MULTIRANGE)
(def ^OID _int4multirange OID/_INT4MULTIRANGE)
(def ^OID nummultirange OID/NUMMULTIRANGE)
(def ^OID _nummultirange OID/_NUMMULTIRANGE)
(def ^OID tsmultirange OID/TSMULTIRANGE)
(def ^OID _tsmultirange OID/_TSMULTIRANGE)
(def ^OID tstzmultirange OID/TSTZMULTIRANGE)
(def ^OID _tstzmultirange OID/_TSTZMULTIRANGE)
(def ^OID datemultirange OID/DATEMULTIRANGE)
(def ^OID _datemultirange OID/_DATEMULTIRANGE)
(def ^OID int8multirange OID/INT8MULTIRANGE)
(def ^OID _int8multirange OID/_INT8MULTIRANGE)
(def ^OID record OID/RECORD)
(def ^OID _record OID/_RECORD)
(def ^OID cstring OID/CSTRING)
(def ^OID _cstring OID/_CSTRING)
(def ^OID any OID/ANY)
(def ^OID anyarray OID/ANYARRAY)
(def ^OID void OID/VOID)
(def ^OID trigger OID/TRIGGER)
(def ^OID event_trigger OID/EVENT_TRIGGER)
(def ^OID language_handler OID/LANGUAGE_HANDLER)
(def ^OID internal OID/INTERNAL)
(def ^OID anyelement OID/ANYELEMENT)
(def ^OID anynonarray OID/ANYNONARRAY)
(def ^OID anyenum OID/ANYENUM)
(def ^OID fdw_handler OID/FDW_HANDLER)
(def ^OID index_am_handler OID/INDEX_AM_HANDLER)
(def ^OID tsm_handler OID/TSM_HANDLER)
(def ^OID table_am_handler OID/TABLE_AM_HANDLER)
(def ^OID anyrange OID/ANYRANGE)
(def ^OID anycompatible OID/ANYCOMPATIBLE)
(def ^OID anycompatiblearray OID/ANYCOMPATIBLEARRAY)
(def ^OID anycompatiblenonarray OID/ANYCOMPATIBLENONARRAY)
(def ^OID anycompatiblerange OID/ANYCOMPATIBLERANGE)
(def ^OID anymultirange OID/ANYMULTIRANGE)
(def ^OID anycompatiblemultirange OID/ANYCOMPATIBLEMULTIRANGE)
(def ^OID pg_brin_bloom_summary OID/PG_BRIN_BLOOM_SUMMARY)
(def ^OID pg_brin_minmax_multi_summary OID/PG_BRIN_MINMAX_MULTI_SUMMARY)
