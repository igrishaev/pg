package com.github.igrishaev;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import com.github.igrishaev.auth.ScramSha256;
import com.github.igrishaev.enums.Phase;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.reducer.IReducer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Accum {

     public static class Node {

         private CopyOutResponse copyOutResponse;
         private PortalSuspended portalSuspended;
         private RowDescription rowDescription;
         private CommandComplete commandComplete;
         private ParseComplete parseComplete;
         private ParameterDescription parameterDescription;
         private Object[] keys;
         private Object acc;

         private boolean isComplete() {
             return commandComplete != null || portalSuspended != null;
         }

         public Object toResult(final ExecuteParams executeParams) {

             if (rowDescription != null) {
                 return executeParams.reducer().finalize(acc);
             }

             final String command = commandComplete.command();

             final String[] parts = command.split(" +");
             final String lead = parts[0];

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
    private ErrorResponse errorResponse;
    private Node current;
    private Throwable exception;
    public ScramSha256.Pipeline scramPipeline;

    public static String[] unifyKeys (final String[] oldKeys) {
        final Map<String, Integer> map = new HashMap<>();
        final String[] newKeys = new String[oldKeys.length];
        for (int i = 0; i < oldKeys.length; i++) {
            final String oldKey = oldKeys[i];
            final int idx = map.getOrDefault(oldKey, 0);
            if (idx == 0) {
                newKeys[i] = oldKey;
                map.put(oldKey, 1);
            }
            else {
                final String newKey = oldKey + "_" + String.valueOf(idx);
                newKeys[i] = newKey;
                map.put(oldKey, idx + 1);
            }
        }
        return newKeys;
    }

    public Accum(final Phase phase, final ExecuteParams executeParams) {
        this.phase = phase;
        this.executeParams = executeParams;
        nodes = new ArrayList<>(2);
        addNode();
    }

    public void setException(final Throwable e) {
        this.exception = e;
    }

    public boolean hasException () {
        return exception != null;
    }

    public void addErrorResponse (final ErrorResponse msg) {
        errorResponse = msg;
    }

    public void handleParameterDescription(final ParameterDescription msg) {
        current.parameterDescription = msg;
    }

    public void handlePortalSuspended(PortalSuspended msg) {
        current.portalSuspended = msg;
        addNode();
    }

    public void handleCopyOutResponse (final CopyOutResponse msg) {
        current.copyOutResponse = msg;
    }

    public RowDescription getRowDescription () {
        return current.rowDescription;
    }

    public ParameterDescription getParameterDescription () {
        return current.parameterDescription;
    }

    public void handleParseComplete(final ParseComplete msg) {
        current.parseComplete = msg;
    }

    public void handleRowDescription(final RowDescription msg) {
        current.acc = executeParams.reducer().initiate();
        current.rowDescription = msg;
        final IFn fnKeyTransform = executeParams.fnKeyTransform();
        final String[] names = unifyKeys(msg.getColumnNames());
        final Object[] keys = new Object[names.length];
        for (short i = 0; i < keys.length; i ++) {
            keys[i] = fnKeyTransform.invoke(names[i]);
        }
        current.keys = keys;
    }

    public void handleCommandComplete (final CommandComplete msg) {
        current.commandComplete = msg;
        addNode();
    }

    // TODO: array?
    public Object getResult () {
        final ArrayList<Object> results = new ArrayList<>(1);
        for (Node node: nodes) {
            if (node.isComplete()) {
                results.add(node.toResult(executeParams));
            }
        }
        return switch (results.size()) {
            case 0 -> null;
            case 1 -> results.get(0);
            default -> results;
        };
    }

    public void setCurrentValues (final Object[] values) {
        final IReducer reducer = executeParams.reducer();
        final Object row = reducer.compose(current.keys, values);
        current.acc = reducer.append(current.acc, row);
    }

    private void addNode() {
        current = new Node();
        nodes.add(current);
    }

    public void maybeThrowError() {
        if (errorResponse == null) {
            if (exception != null) {
                throw new PGError(exception, "Unhandled exception: %s", exception.getMessage());
            }
        }
        else {
            if (exception != null) {
                throw new PGError(exception, "Unhandled exception: %s", exception.getMessage());
            }
            else {
                throw new PGError("ErrorResponse: %s", errorResponse.fields());
            }
        }
    }

    public static void main (final String[] args) {
        final String[] keys = new String[] {"aaa", "bbb", "bbb", "ccc", "bbb"};
        System.out.println(Arrays.toString(unifyKeys(keys)));
    }
}
