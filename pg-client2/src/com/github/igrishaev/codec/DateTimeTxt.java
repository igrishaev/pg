package com.github.igrishaev.codec;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;

public class DateTimeTxt {

    private static final DateTimeFormatter frmt_decode_timestamptz;
    private static final DateTimeFormatter frmt_decode_timestamp;
    private static final DateTimeFormatter frmt_decode_date;

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

        frmt_encode_timestamptz = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSx")
                .withZone(ZoneOffset.UTC);

        frmt_encode_timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                .withZone(ZoneOffset.UTC);

        frmt_encode_date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC);
    }

    public static OffsetDateTime decodeTIMESTAMPTZ (String input) {
        return OffsetDateTime.parse(input, frmt_decode_timestamptz);
    }

    public static LocalDateTime decodeTIMESTAMP (String input) {
        return LocalDateTime.parse(input, frmt_decode_timestamp);
    }

    public static LocalDate decodeDATE (String input) {
        return LocalDate.parse(input, frmt_decode_date);
    }

    public static String encodeTIMESTAMPTZ (Instant instant) {
        return frmt_encode_timestamptz.format(instant);
    }

    public static String encodeTIMESTAMP (Instant instant) {
        return frmt_encode_timestamp.format(instant);
    }

    public static String encodeDATE (Instant instant) {
        return frmt_encode_date.format(instant);
    }
}
