package com.github.igrishaev;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
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

         public Object toResult(ExecuteParams executeParams) {

             if (rowDescription != null) {
                 return executeParams.reducer().finalize(acc);
             }

             String command = commandComplete.command();

             String[] parts = command.split(" +");
             String lead = parts[0];

             return switch (lead) {
                 case "INSERT" -> PersistentHashMap.create(
                         Keyword.intern("inserted"),
                         Integer.parseInt(parts[2])
                 );
                 case "UPDATE" -> PersistentHashMap.create(
                         Keyword.intern("updated"),
                         Integer.parseInt(parts[1])
                 );
                 case "DELETE" -> PersistentHashMap.create(
                         Keyword.intern("deleted"),
                         Integer.parseInt(parts[1])
                 );
                 case "SELECT" -> PersistentHashMap.create(
                         Keyword.intern("selected"),
                         Integer.parseInt(parts[1])
                 );
                 case "COPY" -> PersistentHashMap.create(
                         Keyword.intern("copied"),
                         Integer.parseInt(parts[1])
                 );
                 default -> PersistentHashMap.create(
                         Keyword.intern("command"),
                         command
                 );
             };

         }
    }

    public final Phase phase;
    public final ExecuteParams executeParams;
    private final ArrayList<Node> nodes;
    private final ArrayList<ErrorResponse> errorResponses;
    private Node current;

    public Accum(Phase phase, ExecuteParams executeParams) {
        this.phase = phase;
        this.executeParams = executeParams;
        nodes = new ArrayList<>(2);
        errorResponses = new ArrayList<>(1);
        addNode();
    }

    public void addErrorResponse (ErrorResponse msg) {
        errorResponses.add(msg);
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
        current.acc = executeParams.reducer().initiate();
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
    public Object getResult () {
        final ArrayList<Object> results = new ArrayList<>(1);
        for (Node node: nodes) {
            if (node.commandComplete != null) {
                results.add(node.toResult(executeParams));
            }
        }
        return switch (results.size()) {
            case 0 -> null;
            case 1 -> results.getFirst();
            default -> results;
        };
    }

    public void setCurrentValues (Object[] values) {
        IReducer reducer = executeParams.reducer();
        Object row = reducer.compose(current.keys, values);
        current.acc = reducer.append(current.acc, row);
    }

    private void addNode() {
        current = new Node();
        nodes.add(current);
    }

    public void throwErrorResponse () {
        if (!errorResponses.isEmpty()) {
            ErrorResponse errRes = errorResponses.get(0);
            throw new PGError("ErrorResponse: %s", errRes.fields());
        }
    }
}
