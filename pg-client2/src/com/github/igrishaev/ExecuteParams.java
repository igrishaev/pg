package com.github.igrishaev;

import clojure.lang.IFn;
import com.github.igrishaev.enums.OID;
import com.github.igrishaev.reducer.*;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import clojure.core$identity;
import clojure.core$keyword;

public record ExecuteParams (
        List<Object> params,
        List<OID> OIDs,
        IReducer reducer,
        int rowCount,
        IFn fnKeyTransform,
        OutputStream outputStream,
        boolean binaryEncode,
        boolean binaryDecode
) {

    public static Builder builder() {
        return new Builder();
    }

    public static ExecuteParams standard () {
        return builder().build();
    }

    public static class Builder {

        private List<Object> params = Collections.emptyList();
        private List<OID> OIDs = Collections.emptyList();
        private IReducer reducer = new Default();
        private int rowCount = 0;
        private IFn fnKeyTransform = new core$keyword();
        private OutputStream outputStream = OutputStream.nullOutputStream();
        boolean binaryEncode = false;
        boolean binaryDecode = false;

        public Builder params (List<Object> params) {
            this.params = Objects.requireNonNull(params);
            return this;
        }

        public Builder params (Object... params) {
            this.params = Arrays.asList(params);
            return this;
        }

        public Builder outputStream (OutputStream outputStream) {
            this.outputStream = Objects.requireNonNull(outputStream);
            return this;
        }

        public Builder fnKeyTransform (IFn fnKeyTransform) {
            this.fnKeyTransform = Objects.requireNonNull(fnKeyTransform);
            return this;
        }

        public Builder OIDs (List<OID> OIDs) {
            this.OIDs = Objects.requireNonNull(OIDs);
            return this;
        }

        public Builder reducer (IReducer reducer) {
            this.reducer = Objects.requireNonNull(reducer);
            return this;
        }

        public Builder indexBy (IFn fnIndexBy) {
            this.reducer = new IndexBy(fnIndexBy);
            return this;
        }

        public Builder groupBy (IFn fnGroupBy) {
            this.reducer = new GroupBy(fnGroupBy);
            return this;
        }

        public Builder run (IFn fnRun) {
            this.reducer = new Run(fnRun);
            return this;
        }

        public Builder first () {
            this.reducer = new First();
            return this;
        }

        public Builder KV (IFn fnK, IFn fnV) {
            this.reducer = new KV(fnK, fnV);
            return this;
        }

        public Builder asMatrix () {
            this.reducer = new Matrix();
            return this;
        }

        public Builder fold (IFn fnFold, Object init) {
            this.reducer = new Fold(fnFold, init);
            return this;
        }

        public Builder rowCount (int rowCount) {
            this.rowCount = rowCount;
            return this;
        }

        public Builder binaryEncode (boolean binaryEncode) {
            this.binaryEncode = binaryEncode;
            return this;
        }

        public Builder binaryDecode (boolean binaryDecode) {
            this.binaryDecode = binaryDecode;
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
                    binaryEncode,
                    binaryDecode
            );
        }
    }

    public static void main(String[] args) {
        IFn id = new core$identity();
        System.out.println(id.invoke(42));
        System.out.println(new ExecuteParams.Builder().rowCount(3).build());
    }


}
