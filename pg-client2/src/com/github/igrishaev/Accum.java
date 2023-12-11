package com.github.igrishaev;

import clojure.lang.IFn;
import com.github.igrishaev.enums.Phase;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.reducer.IReducer;
import java.util.ArrayList;

public class Accum {

     public static class Node {

         private CopyOutResponse copyOutResponse;
         private RowDescription rowDescription;
         private CommandComplete commandComplete;
         private ParseComplete parseComplete;
         private ParameterDescription parameterDescription;
         private Object[] keys;
         private Object acc;
         private Object res;

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

    public void handleParameterDescription(ParameterDescription msg) {
        current.parameterDescription = msg;
    }

    public void handleCopyOutResponse (CopyOutResponse msg) {
        current.copyOutResponse = msg;
    }

    public RowDescription getRowDescription () {
        return current.rowDescription;
    }

    public ParameterDescription getParameterDescription () {
        return current.parameterDescription;
    }

    public void handleParseComplete(ParseComplete msg) {
        current.parseComplete = msg;
    }

    public void handleRowDescription(RowDescription msg) {
        current.rowDescription = msg;
        IFn fnKeyTransform = executeParams.fnKeyTransform();
        String[] names = msg.getColumnNames();
        Object[] keys = new Object[names.length];
        for (short i = 0; i < keys.length; i ++) {
            keys[i] = fnKeyTransform.invoke(names[i]);
        }
        current.keys = keys;
    }

    public void handleCommandComplete (CommandComplete msg) {
        current.commandComplete = msg;
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

    private void addNode() {
        IReducer reducer = executeParams.reducer();
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
