package com.github.igrishaev;

import com.github.igrishaev.enums.Phase;
import com.github.igrishaev.msg.CommandComplete;
import com.github.igrishaev.msg.ErrorResponse;
import com.github.igrishaev.msg.ParameterDescription;
import com.github.igrishaev.msg.RowDescription;
import com.github.igrishaev.reducer.IReducer;

import java.util.ArrayList;

public class Result<I, R> {
     public class SubResult {
         private RowDescription rowDescription;
         public CommandComplete commandComplete;
         public ParameterDescription parameterDescription;
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

    public void setParameterDescription(ParameterDescription msg) {
        current.parameterDescription = msg;
    }

    public ParameterDescription getParameterDescription() {
        return current.parameterDescription;
    }

    public RowDescription getRowDescription () {
        return current.rowDescription;
    }

    public ArrayList<R> getResults () {
        final ArrayList<R> results = new ArrayList<>();
        for (SubResult subRes: subResults) {
            if (subRes.commandComplete != null) {
                R result = reducer.finalize(subRes.acc);
                results.add(result);
            }
        }
        return results;
    }

    public R getResult () {
        if (subResults.isEmpty()) {
            return null;
        }
        else {
            SubResult subRes = subResults.get(0);
            if (subRes.commandComplete != null) {
                return reducer.finalize(subRes.acc);
            }
            else {
                return null;
            }
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
        addSubResult();
    }

    public void throwErrorResponse () {
        if (!errorResponses.isEmpty()) {
            ErrorResponse errRes = errorResponses.get(0);
            throw new PGError("Error response: %s", errRes.fields());
        }
    }
}
