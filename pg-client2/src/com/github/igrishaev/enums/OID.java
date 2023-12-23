package com.github.igrishaev.enums;

import com.github.igrishaev.PGError;

import java.util.HashMap;
import java.util.Map;

public enum OID {
    DEFAULT                      (   0),
    BOOL                         (  16),
    _BOOL                        (1000),
    BYTEA                        (  17),
    _BYTEA                       (1001),
    CHAR                         (  18),
    _CHAR                        (1002),
    NAME                         (  19),
    _NAME                        (1003),
    INT8                         (  20),
    _INT8                        (1016),
    INT2                         (  21),
    _INT2                        (1005),
    INT2VECTOR                   (  22),
    _INT2VECTOR                  (1006),
    INT4                         (  23),
    _INT4                        (1007),
    REGPROC                      (  24),
    _REGPROC                     (1008),
    TEXT                         (  25),
    _TEXT                        (1009),
    OID                          (  26),
    _OID                         (1028),
    TID                          (  27),
    _TID                         (1010),
    XID                          (  28),
    _XID                         (1011),
    CID                          (  29),
    _CID                         (1012),
    OIDVECTOR                    (  30),
    _OIDVECTOR                   (1013),
    PG_TYPE                      (  71),
    _PG_TYPE                     ( 210),
    PG_ATTRIBUTE                 (  75),
    _PG_ATTRIBUTE                ( 270),
    PG_PROC                      (  81),
    _PG_PROC                     ( 272),
    PG_CLASS                     (  83),
    _PG_CLASS                    ( 273),
    JSON                         ( 114),
    _JSON                        ( 199),
    XML                          ( 142),
    _XML                         ( 143),
    PG_NODE_TREE                 ( 194),
    PG_NDISTINCT                 (3361),
    PG_DEPENDENCIES              (3402),
    PG_MCV_LIST                  (5017),
    PG_DDL_COMMAND               (  32),
    XID8                         (5069),
    _XID8                        ( 271),
    POINT                        ( 600),
    _POINT                       (1017),
    LSEG                         ( 601),
    _LSEG                        (1018),
    PATH                         ( 602),
    _PATH                        (1019),
    BOX                          ( 603),
    _BOX                         (1020),
    POLYGON                      ( 604),
    _POLYGON                     (1027),
    LINE                         ( 628),
    _LINE                        ( 629),
    FLOAT4                       ( 700),
    _FLOAT4                      (1021),
    FLOAT8                       ( 701),
    _FLOAT8                      (1022),
    UNKNOWN                      ( 705),
    CIRCLE                       ( 718),
    _CIRCLE                      ( 719),
    MONEY                        ( 790),
    _MONEY                       ( 791),
    MACADDR                      ( 829),
    _MACADDR                     (1040),
    INET                         ( 869),
    _INET                        (1041),
    CIDR                         ( 650),
    _CIDR                        ( 651),
    MACADDR8                     ( 774),
    _MACADDR8                    ( 775),
    ACLITEM                      (1033),
    _ACLITEM                     (1034),
    BPCHAR                       (1042),
    _BPCHAR                      (1014),
    VARCHAR                      (1043),
    _VARCHAR                     (1015),
    DATE                         (1082),
    _DATE                        (1182),
    TIME                         (1083),
    _TIME                        (1183),
    TIMESTAMP                    (1114),
    _TIMESTAMP                   (1115),
    TIMESTAMPTZ                  (1184),
    _TIMESTAMPTZ                 (1185),
    INTERVAL                     (1186),
    _INTERVAL                    (1187),
    TIMETZ                       (1266),
    _TIMETZ                      (1270),
    BIT                          (1560),
    _BIT                         (1561),
    VARBIT                       (1562),
    _VARBIT                      (1563),
    NUMERIC                      (1700),
    _NUMERIC                     (1231),
    REFCURSOR                    (1790),
    _REFCURSOR                   (2201),
    REGPROCEDURE                 (2202),
    _REGPROCEDURE                (2207),
    REGOPER                      (2203),
    _REGOPER                     (2208),
    REGOPERATOR                  (2204),
    _REGOPERATOR                 (2209),
    REGCLASS                     (2205),
    _REGCLASS                    (2210),
    REGCOLLATION                 (4191),
    _REGCOLLATION                (4192),
    REGTYPE                      (2206),
    _REGTYPE                     (2211),
    REGROLE                      (4096),
    _REGROLE                     (4097),
    REGNAMESPACE                 (4089),
    _REGNAMESPACE                (4090),
    UUID                         (2950),
    _UUID                        (2951),
    PG_LSN                       (3220),
    _PG_LSN                      (3221),
    TSVECTOR                     (3614),
    _TSVECTOR                    (3643),
    GTSVECTOR                    (3642),
    _GTSVECTOR                   (3644),
    TSQUERY                      (3615),
    _TSQUERY                     (3645),
    REGCONFIG                    (3734),
    _REGCONFIG                   (3735),
    REGDICTIONARY                (3769),
    _REGDICTIONARY               (3770),
    JSONB                        (3802),
    _JSONB                       (3807),
    JSONPATH                     (4072),
    _JSONPATH                    (4073),
    TXID_SNAPSHOT                (2970),
    _TXID_SNAPSHOT               (2949),
    PG_SNAPSHOT                  (5038),
    _PG_SNAPSHOT                 (5039),
    INT4RANGE                    (3904),
    _INT4RANGE                   (3905),
    NUMRANGE                     (3906),
    _NUMRANGE                    (3907),
    TSRANGE                      (3908),
    _TSRANGE                     (3909),
    TSTZRANGE                    (3910),
    _TSTZRANGE                   (3911),
    DATERANGE                    (3912),
    _DATERANGE                   (3913),
    INT8RANGE                    (3926),
    _INT8RANGE                   (3927),
    INT4MULTIRANGE               (4451),
    _INT4MULTIRANGE              (6150),
    NUMMULTIRANGE                (4532),
    _NUMMULTIRANGE               (6151),
    TSMULTIRANGE                 (4533),
    _TSMULTIRANGE                (6152),
    TSTZMULTIRANGE               (4534),
    _TSTZMULTIRANGE              (6153),
    DATEMULTIRANGE               (4535),
    _DATEMULTIRANGE              (6155),
    INT8MULTIRANGE               (4536),
    _INT8MULTIRANGE              (6157),
    RECORD                       (2249),
    _RECORD                      (2287),
    CSTRING                      (2275),
    _CSTRING                     (1263),
    ANY                          (2276),
    ANYARRAY                     (2277),
    VOID                         (2278),
    TRIGGER                      (2279),
    EVENT_TRIGGER                (3838),
    LANGUAGE_HANDLER             (2280),
    INTERNAL                     (2281),
    ANYELEMENT                   (2283),
    ANYNONARRAY                  (2776),
    ANYENUM                      (3500),
    FDW_HANDLER                  (3115),
    INDEX_AM_HANDLER             ( 325),
    TSM_HANDLER                  (3310),
    TABLE_AM_HANDLER             ( 269),
    ANYRANGE                     (3831),
    ANYCOMPATIBLE                (5077),
    ANYCOMPATIBLEARRAY           (5078),
    ANYCOMPATIBLENONARRAY        (5079),
    ANYCOMPATIBLERANGE           (5080),
    ANYMULTIRANGE                (4537),
    ANYCOMPATIBLEMULTIRANGE      (4538),
    PG_BRIN_BLOOM_SUMMARY        (4600),
    PG_BRIN_MINMAX_MULTI_SUMMARY (4601);

    private final int code;

    private final static Map<Integer, OID> intToItem;

    static {
        intToItem = new HashMap<>();
        for (OID oid: values()) {
            intToItem.put(oid.code, oid);
        }
    }

    OID(final int code) {
        this.code = code;
    }

    public static OID ofInt (final int code) {
        final OID result = intToItem.get(code);
        if (result == null) {
            throw new PGError("unknown OID, code: %s", code);
        }
        else {
            return result;
        }
    }

    public int toInt() {
        return code;
    }

}
