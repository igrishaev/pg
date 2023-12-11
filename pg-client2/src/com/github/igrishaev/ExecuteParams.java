package com.github.igrishaev;

import com.github.igrishaev.enums.OID;
import com.github.igrishaev.reducer.Default;
import com.github.igrishaev.reducer.IReducer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ExecuteParams (
        List<Object> params,
        List<OID> OIDs,
        IReducer reducer,
        int rowCount
) {

    public static class Builder {

        private List<Object> params = Collections.emptyList();
        private List<OID> OIDs = Collections.emptyList();
        private IReducer reducer = new Default();
        private int rowCount = 0;

        public Builder params (List<Object> params) {
            this.params = Objects.requireNonNull(params);
            return this;
        }

        public Builder addParam (Object param) {
            this.params.add(param);
            return this;
        }

        public Builder addParams (List<Object> params) {
            this.params.addAll(Objects.requireNonNull(params));
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

        public Builder rowCount (int rowCount) {
            this.rowCount = rowCount;
            return this;
        }

        public ExecuteParams build () {
            return new ExecuteParams(
                    params,
                    OIDs,
                    reducer,
                    rowCount
            );
        }

    }

    public static void main(String[] args) {
        System.out.println(new ExecuteParams.Builder().rowCount(3).build());
    }


}
