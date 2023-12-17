package com.github.igrishaev.copy;

import com.github.igrishaev.Const;
import com.github.igrishaev.enums.CopyFormat;
import com.github.igrishaev.enums.OID;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record CopyParams(
        String CSVNull,
        String CSVCellSep,
        String CSVQuote,
        String CSVLineSep,
        List<OID> OIDs,
        CopyFormat format
) {

    public static CopyParams standard () {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        String CSVNull = Const.COPY_CSV_NULL;
        String CSVCellSep = Const.COPY_CSV_CELL_SEP;
        String CSVQuote = Const.COPY_CSV_CELL_QUOTE;
        String CSVLineSep = Const.COPY_CSV_LINE_SEP;
        List<OID> OIDs = Collections.emptyList();
        CopyFormat format = CopyFormat.CSV;

        public Builder CSVNull (final String CSVNull) {
            this.CSVNull = Objects.requireNonNull(CSVNull);
            return this;
        }

        public Builder CSVCellSep (final String CSVCellSep) {
            this.CSVCellSep = Objects.requireNonNull(CSVCellSep);
            return this;
        }

        public Builder CSVQuote (final String CSVQuote) {
            this.CSVQuote = Objects.requireNonNull(CSVQuote);
            return this;
        }

        public Builder CSVLineSep (final String CSVLineSep) {
            this.CSVLineSep = Objects.requireNonNull(CSVLineSep);
            return this;
        }

        public Builder OIDs (final List<OID> OIDs) {
            this.OIDs = Objects.requireNonNull(OIDs);
            return this;
        }

        public Builder format (final CopyFormat format) {
            this.format = Objects.requireNonNull(format);
            return this;
        }

        public Builder setCSV () {
            this.format = CopyFormat.CSV;
            return this;
        }

        public Builder setBin () {
            this.format = CopyFormat.BIN;
            return this;
        }

        public Builder setTab () {
            this.format = CopyFormat.TAB;
            return this;
        }

        public CopyParams build () {
            return new CopyParams(
                    CSVNull,
                    CSVCellSep,
                    CSVQuote,
                    CSVLineSep,
                    OIDs,
                    format
            );
        }

    }

}
