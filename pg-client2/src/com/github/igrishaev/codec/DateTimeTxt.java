package com.github.igrishaev.codec;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Date;

public class DateTimeTxt {

    private static final DateTimeFormatter frmt_decode_timestamptz;
    private static final DateTimeFormatter frmt_decode_timestamp;
    private static final DateTimeFormatter frmt_decode_date;
    private static final DateTimeFormatter frmt_decode_timetz;
    private static final DateTimeFormatter frmt_decode_time;

    private static final DateTimeFormatter frmt_encode_timestamptz;
    private static final DateTimeFormatter frmt_encode_timestamp;
    private static final DateTimeFormatter frmt_encode_date;

    static {
        frmt_decode_timestamptz = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .appendPattern("x")
                .toFormatter()
                .withZone(ZoneOffset.UTC);

        frmt_decode_timestamp = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .toFormatter();

        frmt_decode_date = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .toFormatter();

        frmt_decode_timetz = new DateTimeFormatterBuilder()
                .appendPattern("HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .appendPattern("x")
                .toFormatter()
                .withZone(ZoneOffset.UTC);

        frmt_decode_time = new DateTimeFormatterBuilder()
                .appendPattern("HH:mm:ss")
                .toFormatter();

        frmt_encode_timestamptz = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSx")
                .withZone(ZoneOffset.UTC);

        frmt_encode_timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                .withZone(ZoneOffset.UTC);

        frmt_encode_date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC);
    }

    //
    // Decoding
    //
    public static OffsetDateTime decodeTIMESTAMPTZ (String input) {
        return OffsetDateTime.parse(input, frmt_decode_timestamptz);
    }

    public static LocalDateTime decodeTIMESTAMP (String input) {
        return LocalDateTime.parse(input, frmt_decode_timestamp);
    }

    public static LocalDate decodeDATE (String input) {
        return LocalDate.parse(input, frmt_decode_date);
    }

    public static OffsetTime decodeTIMETZ (String input) {
        return OffsetTime.parse(input, frmt_decode_timetz);
    }

    public static LocalTime decodeTIME (String input) {
        return LocalTime.parse(input, frmt_decode_time);
    }

    //
    // Encoding
    //

    // Date
    public static String encodeTIMESTAMPTZ (Date date) {
        return encodeTIMESTAMPTZ(date.toInstant());
    }

    public static String encodeTIMESTAMP (Date date) {
        return encodeTIMESTAMP(date.toInstant());
    }

    public static String encodeDATE (Date date) {
        return encodeDATE(date.toInstant());
    }

    // Temporal
    public static String encodeTIMESTAMPTZ (Temporal t) {
        return frmt_encode_timestamptz.format(t);
    }

    public static String encodeTIMESTAMP (Temporal t) {
        return frmt_encode_timestamp.format(t);
    }

    public static String encodeDATE (Temporal t) {
        return frmt_encode_date.format(t);
    }
    
}
