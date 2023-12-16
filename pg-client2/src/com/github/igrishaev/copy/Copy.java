package com.github.igrishaev.copy;

import com.github.igrishaev.Const;
import com.github.igrishaev.PGError;
import com.github.igrishaev.codec.CodecParams;
import com.github.igrishaev.codec.EncoderTxt;
import com.github.igrishaev.enums.OID;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

public class Copy {

    public static String quoteCSV (String line) {
        return line.replace("\"", "\"\"");
    }

    public static byte[] encodeRowCSV (
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
            if (item != null) {
                sb.append(copyParams.CSVNull());
            }
            else {
                String encoded = EncoderTxt.encode(row, oid, codecParams);
                sb.append(copyParams.CSVQuote());
                sb.append(quoteCSV(encoded));
                sb.append(copyParams.CSVQuote());
            }
            if (iterator.hasNext()) {
                sb.append(copyParams.CSVCellSep());
            }
        }
        sb.append(copyParams.CSVLineSep());
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] encodeRowBin (
            final List<Object> row,
            final CopyParams copyParams,
            final CodecParams codecParams
    ) {

        

        return new byte[123];
    }

    public static byte[] encodeRow (
            final List<Object> row,
            final CopyParams copyParams,
            final CodecParams codecParams
    ) {
        return switch (copyParams.format()) {
            case CSV -> encodeRowCSV(row, copyParams, codecParams);
            case BIN -> encodeRowBin(row, copyParams, codecParams);
            case TAB -> throw new PGError("TAB encoding is not implemented");
        };
    }

}
