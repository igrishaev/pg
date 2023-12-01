package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import com.github.igrishaev.IReducer;

public class Result<I, R> {

     public class SubResult {
         public ParameterDescription _ParameterDescription;
         public RowDescription _RowDescription;
         public CommandComplete _CommandComplete;
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

    public void addDataRow (DataRow msg) {

        RowDescription rd = current._RowDescription;
        short valueCount = rd.columnCount();
        RowDescription.Column[] columns = rd.columns();
        ByteBuffer[] values = msg.values();

        HashMap<String, Integer> row = new HashMap<>();

        for (short i = 0; i < valueCount; i++) {
            ByteBuffer buf = values[i];
            String payload = "123123";
            int value = Integer.parseInt(payload);
            // int value = 1;
            String field = columns[i].name();
            row.put(field, value);
        }
        current.acc = reducer.append(current.acc, new Object[1], new Object[1]);
    }

    public ParameterDescription getParameterDescription () {
        return current._ParameterDescription;
    }

    public RowDescription getRowDescription () {
        return current._RowDescription;
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

    public void addRowDescription (RowDescription msg) {
        current._RowDescription = msg;
    }

    public void addCommandComplete (CommandComplete msg) {
        current._CommandComplete = msg;
    }

}
