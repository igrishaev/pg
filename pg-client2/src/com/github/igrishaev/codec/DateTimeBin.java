package com.github.igrishaev.codec;

import java.nio.ByteBuffer;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

public class DateTimeBin {

    private static final Duration PG_DIFF;

    static {
        PG_DIFF = Duration.between(
                Instant.EPOCH,
                LocalDate.of(2000, 1, 1).atStartOfDay(ZoneOffset.UTC)
        );
    }

    //
    // Decode
    //
    public static OffsetDateTime decodeTIMESTAMPTZ (ByteBuffer buf) {
        long secsAndMicros = buf.getLong();
        long secs = secsAndMicros / 1_000_000 + PG_DIFF.toSeconds();
        long nanoSec = secsAndMicros % 1_000_000 * 1_000;
        Instant inst = Instant.ofEpochSecond(secs, nanoSec);
        return OffsetDateTime.ofInstant(inst, ZoneOffset.UTC);
    }

    public static LocalDateTime decodeTIMESTAMP (ByteBuffer buf) {
        long secsAndMicros = buf.getLong();
        long secs = secsAndMicros / 1_000_000 + PG_DIFF.toSeconds();
        long nanoSec = secsAndMicros % 1_000_000 * 1_000;
        return LocalDateTime.ofEpochSecond(secs, (int)nanoSec, ZoneOffset.UTC);
    }

    public static LocalDate decodeDATE (ByteBuffer buf) {
        int days = buf.getInt();
        return LocalDate.ofEpochDay(days + PG_DIFF.toDays());
    }

    public static OffsetTime decodeTIMETZ (ByteBuffer buf) {
        long micros = buf.getLong();
        int offset = buf.getInt();
        return OffsetTime.of(
                LocalTime.ofNanoOfDay(micros * 1_000),
                ZoneOffset.ofTotalSeconds(-offset)
        );
    }

    public static LocalTime decodeTIME (ByteBuffer buf) {
        long micros = buf.getLong();
        return LocalTime.ofNanoOfDay(micros * 1_000);
    }

    //
    // Encode
    //
    public static ByteBuffer encodeTIME (Temporal t) {
        long micros = t.getLong(ChronoField.MICRO_OF_DAY);
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(micros);
        return buf;
    }

    public static ByteBuffer encodeTIMETZ (Temporal t) {
        long micros = t.getLong(ChronoField.MICRO_OF_DAY);
        long offset = -t.getLong(ChronoField.OFFSET_SECONDS);
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putLong(micros);
        buf.putInt((int)offset);
        return buf;
    }

    public static ByteBuffer encodeDATE (Temporal t) {
        long days = t.getLong(ChronoField.EPOCH_DAY) - PG_DIFF.toDays();
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt((int)days);
        return buf;
    }

    public static ByteBuffer encodeTIMESTAMP (Temporal t) {
        long secs = t.getLong(ChronoField.INSTANT_SECONDS) - PG_DIFF.toSeconds();
        long micros = t.getLong(ChronoField.MICRO_OF_SECOND);
        long sum = secs * 1_000_000 + micros;
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(sum);
        return buf;
    }

    public static ByteBuffer encodeTIMESTAMPTZ (Temporal t) {
        long secs = t.getLong(ChronoField.INSTANT_SECONDS) - PG_DIFF.toSeconds();
        long micros = t.getLong(ChronoField.MICRO_OF_SECOND);
        long sum = secs * 1_000_000 + micros;
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(sum);
        return buf;
    }

}
