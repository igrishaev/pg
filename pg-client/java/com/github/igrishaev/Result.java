package com.github.igrishaev;

import java.util.ArrayList;

public class Result<I, R> {
     public class SubResult {
         private RowDescription rowDescription;
         public CommandComplete commandComplete;
         public Object[] keys;
         public I acc;
    }

    public final Phase phase;
    public ArrayList<SubResult> subResults;
    public ArrayList<ErrorResponse> errorResponses;
    public SubResult current;
    public IReducer<I, R> reducer;

    public Result (Phase phase, IReducer<I, R> reducer) {
        this.phase = phase;
        this.reducer = reducer;
        subResults = new ArrayList<>();
        errorResponses = new ArrayList<>();
        addSubResult();
    }

    public void setRowDescription(RowDescription msg) {
        current.rowDescription = msg;
    }

    public RowDescription getRowDescription () {
        return current.rowDescription;
    }

    public ArrayList<R> getResults () {
        final ArrayList<R> results = new ArrayList<>();
        for (SubResult subRes: subResults) {
            R result = reducer.finalize(subRes.acc);
            results.add(result);
        }
        return results;
    }

    public R getResult () {
        if (subResults.isEmpty()) {
            return null;
        }
        else {
            return reducer.finalize(subResults.get(0).acc);
        }
    }

    public void addErrorResponse (ErrorResponse msg) {
        errorResponses.add(msg);
    }

    public void setCurrentValues (Object[] values) {
        current.acc = reducer.append(current.acc, current.keys, values);
    }

    public void addSubResult () {
        current = new SubResult();
        if (reducer != null) {
            current.acc = reducer.initiate();
        }
        subResults.add(current);
    }

    public void setCurrentKeys (Object[] keys) {
        current.keys = keys;
    }

    public void setCommandComplete (CommandComplete msg) {
        current.commandComplete = msg;
    }

    public void throwErrorResponse () {
        if (!errorResponses.isEmpty()) {
            ErrorResponse errRes = errorResponses.get(0);
            throw new PGError("Error response: %s", errRes.fields());
        }
    }
}
