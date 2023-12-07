package com.github.igrishaev;

import com.github.igrishaev.enums.Phase;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.reducer.IReducer;
import java.io.OutputStream;

import java.util.ArrayList;

public class Accum {

     public static class Node {
         public OutputStream outputStream;
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

    public final Phase phase;
    public ArrayList<Node> nodes;
    public ArrayList<ErrorResponse> errorResponses;
    public Node current;
    public IReducer reducer;

    public Accum(Phase phase, IReducer reducer) {
        this.phase = phase;
        this.reducer = reducer;
        nodes = new ArrayList<>();
        errorResponses = new ArrayList<>();
        addNode();
    }

    public ArrayList<Result> getResults () {
        final ArrayList<Result> results = new ArrayList<>();
        for (Node node: nodes) {
            if (node.commandComplete != null) {
                node.res = reducer.finalize(node.acc);
                results.add(node.toResult());
            }
        }
        return results;
    }

    public Result getResult () {
        return getResults().get(0);
    }

    public void setCurrentValues (Object[] values) {
        Object row = reducer.compose(current.keys, values);
        current.acc = reducer.append(current.acc, row);
    }

    public void addNode() {
        current = new Node();
        if (reducer != null) {
            current.acc = reducer.initiate();
        }
        nodes.add(current);
    }

    public void throwErrorResponse () {
        if (!errorResponses.isEmpty()) {
            ErrorResponse errRes = errorResponses.get(0);
            throw new PGError("Error response: %s", errRes.fields());
        }
    }
}
