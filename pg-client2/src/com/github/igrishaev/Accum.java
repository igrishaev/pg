package com.github.igrishaev;

import clojure.lang.IFn;
import com.github.igrishaev.enums.Phase;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.reducer.IReducer;
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

    public final Phase phase;
    public final ArrayList<Node> nodes;
    public final ArrayList<ErrorResponse> errorResponses;
    public final ExecuteParams executeParams;
    public Node current;

    public Accum(Phase phase, ExecuteParams executeParams) {
        this.phase = phase;
        this.executeParams = executeParams;
        nodes = new ArrayList<>(2);
        errorResponses = new ArrayList<>(1);
        addNode();
    }

    // TODO: array?
    public ArrayList<Result> getResults () {
        IReducer reducer = executeParams.reducer();
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
        IReducer reducer = executeParams.reducer();
        Object row = reducer.compose(current.keys, values);
        current.acc = reducer.append(current.acc, row);
    }

    public void addNode() {
        IReducer reducer = executeParams.reducer();
        current = new Node();
        current.acc = reducer.initiate();
        nodes.add(current);
    }

    public void setKeys (String[] keys) {
        IFn fnKeyTransform = executeParams.fnKeyTransform();
        Object[] newKeys = new Object[keys.length];
        for (short i = 0; i < keys.length; i ++) {
            newKeys[i] = fnKeyTransform.invoke(keys[i]);
        }
        current.keys = newKeys;
    }

    public void throwErrorResponse () {
        if (!errorResponses.isEmpty()) {
            ErrorResponse errRes = errorResponses.get(0);
            throw new PGError("ErrorResponse: %s", errRes.fields());
        }
    }
}
