package com.github.igrishaev.copy;

import com.github.igrishaev.Const;
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

    public static String quoteCSV (String line) {
        return line.replace("\"", "\"\"");
    }

    public static String encodeRowCSV (
            final List<Object> row,
            final CopyParams copyParams,
            final CodecParams codecParams
    ) {
        final StringBuilder sb = new StringBuilder();
        final Iterator<Object> iterator = row.iterator();
        final List<OID> OIDs = copyParams.OIDs();
        final int OIDLen = OIDs.size();
        short i = 0;
        while (iterator.hasNext()) {
            OID oid = i < OIDLen ? OIDs.get(i) : OID.DEFAULT;
            i++;
            Object item = iterator.next();
            if (item == null) {
                sb.append(copyParams.CSVNull());
            }
            // TODO: check if needs quoting? perf test
            else {
                String encoded = EncoderTxt.encode(item, oid, codecParams);
                sb.append(copyParams.CSVQuote());
                sb.append(quoteCSV(encoded));
                sb.append(copyParams.CSVQuote());
            }
            if (iterator.hasNext()) {
                sb.append(copyParams.CSVCellSep());
            }
        }
        sb.append(copyParams.CSVLineSep());
        return sb.toString();
    }

    public static ByteBuffer encodeRowBin (
            final List<Object> row,
            final CopyParams copyParams,
            final CodecParams codecParams
    ) {

        final short count = (short) row.size();
        final ByteBuffer[] bufs = new ByteBuffer[count];

        final List<OID> OIDs = copyParams.OIDs();
        final int OIDLen = OIDs.size();

        int totalSize = 2;

        // TODO: non-needed allocations (prefill header bytes)
        for (short i = 0; i < count; i++) {
            final Object item = row.get(i);
            if (item == null) {
                totalSize += 4;
                bufs[i] = null;
            }
            else {
                OID oid = i < OIDLen ? OIDs.get(i) : OID.DEFAULT;
                ByteBuffer buf = EncoderBin.encode(item, oid, codecParams);
                totalSize += 4 + buf.array().length;
                bufs[i] = buf;
            }
        }

        ByteBuffer result = ByteBuffer.allocate(totalSize);
        result.putShort(count);

        for (ByteBuffer buf: bufs) {
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

    public static void main(String[] args) {
        System.out.println(encodeRowCSV(
                List.of(1, 2, 3),
                CopyParams.standard(),
                CodecParams.standard())
        );

        List<Object> row = new ArrayList<>();
        row.add(1);
        row.add("Ivan");
        row.add(true);
        row.add(null);

        List<OID> OIDs = List.of(OID.INT2, OID.DEFAULT, OID.BOOL);

        System.out.println(
                Arrays.toString(
                    encodeRowBin(
                            row,
                            CopyParams.builder().OIDs(OIDs).build(),
                            CodecParams.standard()
                    ).array())
        );

        System.out.println(Arrays.toString(Const.COPY_BIN_HEADER));

        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort((short)-1);
        System.out.println(Arrays.toString(bb.array()));
    }

}
