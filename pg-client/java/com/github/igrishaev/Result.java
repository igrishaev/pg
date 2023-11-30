package com.github.igrishaev;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class Result {

     public static class SubResult {

        private ParameterDescription _ParameterDescription;
        private RowDescription _RowDescription;
        private CommandComplete _CommandComplete;
        private Object acc;
        private ArrayList<Object> acc2;

    }

    public final String phase;
    public ArrayList<SubResult> subResults;
    public ArrayList<ErrorResponse> errorResponses;
    public SubResult current;
    public IReducer<Object,Object> reducer;

    public Result (String phase, IReducer reducer) {
        this.phase = phase;
        this.reducer = reducer;
        subResults = new ArrayList<>();
        errorResponses = new ArrayList<>();
        addSubResult();
    }

    public ArrayList<Object> getResults () {

        if (!errorResponses.isEmpty()) {
            ErrorResponse errRes = errorResponses.get(0);
            throw new PGError("Error response: %s", errRes);
        }

        ArrayList<Object> results = new ArrayList<>();

        for (SubResult subRes: subResults) {
            Object result = reducer.finalize(subRes.acc);
            results.add(result);
        }

        return results;
    }

    public void addErrorResponse (ErrorResponse msg) {
        errorResponses.add(msg);
    }

    public void addDataRow (DataRow msg) {

        //RowDescription rd = current._RowDescription;
        //short valueCount = rd.columnCount();

        //HashMap<String, Integer> row = new HashMap<>();

        //for (short i = 0; i < valueCount; i++) {
        //  ByteBuffer buf = msg.values().get(i);
        //  // String payload = "123123";
        //  // int value = Integer.parseInt(payload);
        //  int value = 1;
        //  String field = rd.columns().get(i).name();
        //  row.put(field, value);
        //}

        current.acc2.add(1);
        // current.acc = reducer.append(current.acc, row);
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
        current.acc2 = new ArrayList<Object>();
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
