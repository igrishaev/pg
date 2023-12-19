package com.github.igrishaev;

import clojure.lang.IFn;
import com.github.igrishaev.enums.CopyFormat;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.reducer.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import clojure.core$identity;
import clojure.core$keyword;

public record ExecuteParams (
        List<Object> params,
        List<OID> OIDs,
        IReducer reducer,
        int rowCount,
        IFn fnKeyTransform,
        OutputStream outputStream,
        InputStream inputStream,
        boolean binaryEncode,
        boolean binaryDecode,
        String CSVNull,
        String CSVCellSep,
        String CSVQuote,
        String CSVLineSep,
        CopyFormat copyFormat,
        int copyBufSize,
        List<List<Object>> copyInRows,
        List<Map<Object, Object>> copyInMaps,
        List<Object> copyMapKeys
) {

    public static Builder builder() {
        return new Builder();
    }

    public static ExecuteParams standard () {
        return new Builder().build();
    }

    public static class Builder {

        private List<Object> params = Collections.emptyList();
        private List<OID> OIDs = Collections.emptyList();
        private IReducer reducer = new Default();
        private int rowCount = 0;
        private IFn fnKeyTransform = new core$keyword();
        private OutputStream outputStream = OutputStream.nullOutputStream();
        private InputStream inputStream = InputStream.nullInputStream();
        private boolean binaryEncode = false;
        private boolean binaryDecode = false;
        private String CSVNull = Const.COPY_CSV_NULL;
        private String CSVCellSep = Const.COPY_CSV_CELL_SEP;
        private String CSVQuote = Const.COPY_CSV_CELL_QUOTE;
        private String CSVLineSep = Const.COPY_CSV_LINE_SEP;
        private CopyFormat copyFormat = CopyFormat.CSV;
        private int copyBufSize = Const.COPY_BUFFER_SIZE;
        private List<List<Object>> copyInRows = Collections.emptyList();
        private List<Map<Object, Object>> copyInMaps = Collections.emptyList();
        List<Object> copyMapKeys = Collections.emptyList();

        public Builder params (final List<Object> params) {
            this.params = Objects.requireNonNull(params);
            return this;
        }

        public Builder copyInRows (final List<List<Object>> copyInRows) {
            this.copyInRows = Objects.requireNonNull(copyInRows);
            return this;
        }

        public Builder copyInMaps (final List<Map<Object, Object>> copyInMaps) {
            this.copyInMaps = Objects.requireNonNull(copyInMaps);
            return this;
        }

        // TODO: rename copyInKeys
        public Builder copyMapKeys (final List<Object> copyMapKeys) {
            this.copyMapKeys = Objects.requireNonNull(copyMapKeys);
            return this;
        }

        public Builder params (Object... params) {
            this.params = Arrays.asList(params);
            return this;
        }

        public Builder outputStream (final OutputStream outputStream) {
            this.outputStream = Objects.requireNonNull(outputStream);
            return this;
        }

        public Builder inputStream (final InputStream inputStream) {
            this.inputStream = Objects.requireNonNull(inputStream);
            return this;
        }

        public Builder fnKeyTransform (final IFn fnKeyTransform) {
            this.fnKeyTransform = Objects.requireNonNull(fnKeyTransform);
            return this;
        }

        public Builder OIDs (final List<OID> OIDs) {
            this.OIDs = Objects.requireNonNull(OIDs);
            return this;
        }

        public Builder reducer (final IReducer reducer) {
            this.reducer = Objects.requireNonNull(reducer);
            return this;
        }

        public Builder indexBy (final IFn fnIndexBy) {
            this.reducer = new IndexBy(fnIndexBy);
            return this;
        }

        public Builder groupBy (final IFn fnGroupBy) {
            this.reducer = new GroupBy(fnGroupBy);
            return this;
        }

        public Builder run (final IFn fnRun) {
            this.reducer = new Run(fnRun);
            return this;
        }

        public Builder first () {
            this.reducer = new First();
            return this;
        }

        public Builder KV (final IFn fnK, final IFn fnV) {
            this.reducer = new KV(fnK, fnV);
            return this;
        }

        public Builder asMatrix () {
            this.reducer = new Matrix();
            return this;
        }

        public Builder fold (final IFn fnFold, final Object init) {
            this.reducer = new Fold(fnFold, init);
            return this;
        }

        public Builder rowCount (final int rowCount) {
            this.rowCount = rowCount;
            return this;
        }

        public Builder binaryEncode (final boolean binaryEncode) {
            this.binaryEncode = binaryEncode;
            return this;
        }

        public Builder binaryDecode (final boolean binaryDecode) {
            this.binaryDecode = binaryDecode;
            return this;
        }

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

        public Builder copyFormat (final CopyFormat format) {
            this.copyFormat = Objects.requireNonNull(format);
            return this;
        }

        public Builder setCSV () {
            this.copyFormat = CopyFormat.CSV;
            return this;
        }

        public Builder setBin () {
            this.copyFormat = CopyFormat.BIN;
            return this;
        }

        public Builder setTab () {
            this.copyFormat = CopyFormat.TAB;
            return this;
        }

        // TODO: rename to copyBufSize
        public Builder bufSize (final int bufSize) {
            this.copyBufSize = bufSize;
            return this;
        }

        public ExecuteParams build () {
            return new ExecuteParams(
                    params,
                    OIDs,
                    reducer,
                    rowCount,
                    fnKeyTransform,
                    outputStream,
                    inputStream,
                    binaryEncode,
                    binaryDecode,
                    CSVNull,
                    CSVCellSep,
                    CSVQuote,
                    CSVLineSep,
                    copyFormat,
                    copyBufSize,
                    copyInRows,
                    copyInMaps,
                    copyMapKeys
            );
        }
    }

    public static void main(String[] args) {
        IFn id = new core$identity();
        System.out.println(id.invoke(42));
        System.out.println(new ExecuteParams.Builder().rowCount(3).build());
    }


}
