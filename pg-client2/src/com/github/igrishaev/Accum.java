package com.github.igrishaev;

import com.github.igrishaev.enums.Phase;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.reducer.IReducer;

import java.io.OutputStream;
import java.util.ArrayList;

public class Accum {

     public static class Node {

         public CopyOutResponse copyOutResponse;
         public RowDescription rowDescription;
         public CommandComplete commandComplete;
         public ParseComplete parseComplete;
         public ParameterDescription parameterDescription;
         public Object[] keys;
         public Object acc;
         public Object res;

         public Result toResult() {

             String[] parts = commandComplete.tag().split(" +");
             String lead = parts[0];
             int rowsProcessed = switch (lead) {
                 case "INSERT" -> Integer.parseInt(parts[2]);
                 case "UPDATE", "DELETE", "SELECT", "COPY" -> Integer.parseInt(parts[1]);
                 default -> 0;
             };

             return new Result(
                     commandComplete.tag(),
                     rowsProcessed,
                     res
             );
         }
    }

    public OutputStream outputStream;
    public Phase phase;
    public ArrayList<Node> nodes;
    public ArrayList<ErrorResponse> errorResponses;
    public Node current;
    public IReducer reducer;

    public Accum(Phase phase, IReducer reducer, OutputStream outputStream) {
        this.phase = phase;
        this.reducer = reducer;
        this.outputStream = outputStream;
        nodes = new ArrayList<>(2);
        errorResponses = new ArrayList<>(1);
        addNode();
    }

    // TODO: array?
    public ArrayList<Result> getResults () {
        final ArrayList<Result> results = new ArrayList<>(1);
        for (Node node: nodes) {
            if (node.commandComplete != null) {
                if (phase == Phase.COPY) {
                    node.res = node.copyOutResponse;
                }
                else {
                    node.res = reducer.finalize(node.acc);
                }
                results.add(node.toResult());
            }
        }
        return results;
    }

    public void setCurrentValues (Object[] values) {
        Object row = reducer.compose(current.keys, values);
        current.acc = reducer.append(current.acc, row);
    }

    public void addNode() {
        current = new Node();
        current.acc = reducer.initiate();
        nodes.add(current);
    }

    public void throwErrorResponse () {
        if (!errorResponses.isEmpty()) {
            ErrorResponse errRes = errorResponses.get(0);
            throw new PGError("ErrorResponse: %s", errRes.fields());
        }
    }
}
