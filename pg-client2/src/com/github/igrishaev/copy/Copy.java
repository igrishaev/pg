package com.github.igrishaev.copy;

import com.github.igrishaev.ExecuteParams;
import com.github.igrishaev.codec.CodecParams;
import com.github.igrishaev.codec.EncoderBin;
import com.github.igrishaev.codec.EncoderTxt;
import com.github.igrishaev.enums.OID;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Copy {

    public static final byte[] COPY_BIN_HEADER = {
            // header
            (byte) 'P',
            (byte) 'G',
            (byte) 'C',
            (byte) 'O',
            (byte) 'P',
            (byte) 'Y',
            (byte) 10,
            (byte) 0xFF,
            (byte) 13,
            (byte) 10,
            (byte) 0,
            // 0 int32
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            // 0 int32
            (byte) 0, (byte) 0, (byte) 0, (byte) 0
    };

    public static final byte[] MSG_COPY_BIN_TERM = new byte[] {
            (byte) 'd',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 6,
            (byte) -1,
            (byte) -1
    };

    public static String quoteCSV (final String line) {
        return line.replace("\"", "\"\"");
    }

    public static String encodeRowCSV (
            final List<Object> row,
            final ExecuteParams executeParams,
            final CodecParams codecParams
    ) {
        final StringBuilder sb = new StringBuilder();
        final Iterator<Object> iterator = row.iterator();
        final List<OID> OIDs = executeParams.OIDs();
        final int OIDLen = OIDs.size();
        short i = 0;
        while (iterator.hasNext()) {
            final OID oid = i < OIDLen ? OIDs.get(i) : OID.DEFAULT;
            i++;
            final Object item = iterator.next();
            if (item == null) {
                sb.append(executeParams.CSVNull());
            }
            else {
                final String encoded = EncoderTxt.encode(item, oid, codecParams);
                sb.append(executeParams.CSVQuote());
                sb.append(quoteCSV(encoded));
                sb.append(executeParams.CSVQuote());
            }
            if (iterator.hasNext()) {
                sb.append(executeParams.CSVCellSep());
            }
        }
        sb.append(executeParams.CSVLineSep());
        return sb.toString();
    }

    public static ByteBuffer encodeRowBin (
            final List<Object> row,
            final ExecuteParams executeParams,
            final CodecParams codecParams
    ) {
        final short count = (short) row.size();
        final ByteBuffer[] bufs = new ByteBuffer[count];
        final List<OID> OIDs = executeParams.OIDs();
        final int OIDLen = OIDs.size();

        int totalSize = 2;

        int i = -1;
        for (final Object item: row) {
            i++;
            if (item == null) {
                totalSize += 4;
                bufs[i] = null;
            }
            else {
                final OID oid = i < OIDLen ? OIDs.get(i) : OID.DEFAULT;
                final ByteBuffer buf = EncoderBin.encode(item, oid, codecParams);
                totalSize += 4 + buf.array().length;
                bufs[i] = buf;
            }
        }

        final ByteBuffer result = ByteBuffer.allocate(totalSize);
        result.putShort(count);

        for (final ByteBuffer buf: bufs) {
            if (buf == null) {
                result.putInt(-1);
            }
            else {
                result.putInt(buf.limit());
                result.put(buf.rewind());
            }
        }
        return result;
    }

    public static void main(final String[] args) {
        System.out.println(encodeRowCSV(
                List.of(1, 2, 3),
                ExecuteParams.standard(),
                CodecParams.standard())
        );

        final List<Object> row = new ArrayList<>();
        row.add(1);
        row.add("Ivan");
        row.add(true);
        row.add(null);

        final List<OID> OIDs = List.of(OID.INT2, OID.DEFAULT, OID.BOOL);

        System.out.println(
                Arrays.toString(
                    encodeRowBin(
                            row,
                            ExecuteParams.builder().OIDs(OIDs).build(),
                            CodecParams.standard()
                    ).array())
        );

        System.out.println(Arrays.toString(Copy.COPY_BIN_HEADER));

        final ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort((short)-1);
        System.out.println(Arrays.toString(bb.array()));
    }

}
