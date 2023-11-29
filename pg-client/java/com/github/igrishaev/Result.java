package com.github.igrishaev;

import java.util.ArrayList;

public class Result {

    class SubResult {

        private ParameterDescription _ParameterDescription;
        private RowDescription _RowDescription;
        private CommandComplete _CommandComplete;
        private Object acc;

    }

    public final String phase;
    public ArrayList<SubResult> subResults;
    public ArrayList<ErrorResponse> errorResponses;
    public SubResult current;
    public IReducer reducer;

    public ArrayList<Object> getResults () {
        ArrayList<Object> results = new ArrayList<Object>();

        for (SubResult subRes: subResults) {
            results.add(reducer.finalize(subRes.acc));
        }

        return results;
    }

    public Result (String phase, IReducer reducer) {
        this.phase = phase;
        this.reducer = reducer;
        errorResponses = new ArrayList<ErrorResponse>();
        addSubResult();
    }

    public void addDataRow (DataRow msg) {
        current.acc = reducer.append(current.acc, msg);
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
