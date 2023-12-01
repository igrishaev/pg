package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import com.github.igrishaev.IReducer;

public class Result<I, R> {

     public class SubResult {
         public ParameterDescription _ParameterDescription;
         public CommandComplete _CommandComplete;
         public Object[] keys;
         public OID[] oids;
         public I acc;
    }

    public final String phase;
    public ArrayList<SubResult> subResults;
    public ArrayList<ErrorResponse> errorResponses;
    public SubResult current;
    public IReducer<I, R> reducer;

    public Result (String phase, IReducer<I, R> reducer) {
        this.phase = phase;
        this.reducer = reducer;
        subResults = new ArrayList<>();
        errorResponses = new ArrayList<>();
        addSubResult();
    }

    public ArrayList<R> getResults () {
        if (!errorResponses.isEmpty()) {
            ErrorResponse errRes = errorResponses.get(0);
            throw new PGError("Error response: %s", errRes.fields());
        }
        final ArrayList<R> results = new ArrayList<>();
        for (SubResult subRes: subResults) {
            R result = reducer.finalize(subRes.acc);
            results.add(result);
        }
        return results;
    }

    public void addErrorResponse (ErrorResponse msg) {
        errorResponses.add(msg);
    }

    public void setCurrentValues (Object[] values) {
        current.acc = reducer.append(current.acc, current.keys, values);
    }

    public ParameterDescription getParameterDescription () {
        return current._ParameterDescription;
    }

    public CommandComplete getCommandComplete () {
        return current._CommandComplete;
    }

    public void addSubResult () {
        current = new SubResult();
        current.acc = reducer.initiate();
        subResults.add(current);
    }

    public void addParameterDescription (ParameterDescription msg) {
        current._ParameterDescription = msg;
    }

    public OID[] getCurrentOIDs () {
        return current.oids;
    }

    public void setCurrentOIDs (OID[] oids) {
        current.oids = oids;
    }

    public void setCurrentKeys (Object[] keys) {
        current.keys = keys;
    }

    public void addCommandComplete (CommandComplete msg) {
        current._CommandComplete = msg;
    }

}
