package com.github.igrishaev;

import com.github.igrishaev.enums.Phase;
import com.github.igrishaev.msg.*;
import com.github.igrishaev.reducer.IReducer;

import java.util.ArrayList;

public class Accum<I, R> {
     public class Node {
         public RowDescription rowDescription;
         public CommandComplete commandComplete;
         public ParseComplete parseComplete;
         public ParameterDescription parameterDescription;
         public Object[] keys;
         public I acc;

         public Result toResult() {

             int selectedCount = 0;
             int insertedCount = 0;
             int updatedCount = 0;
             int deletedCount = 0;
             int copyCount = 0;

             String[] parts = commandComplete.tag().split("\s+");
             String lead = parts[0];
             switch (lead) {
                 case "INSERT":
                     insertedCount = Integer.parseInt(parts[2]);
                     break;
                 case "UPDATE":
                     updatedCount = Integer.parseInt(parts[1]);
                     break;
                 case "DELETE":
                     deletedCount = Integer.parseInt(parts[1]);
                 case "SELECT":
                     selectedCount = Integer.parseInt(parts[1]);
                 case "COPY":
                     copyCount = Integer.parseInt(parts[1]);
             }

             return new Result(
                     commandComplete.tag(),
                     selectedCount,
                     insertedCount,
                     updatedCount,
                     deletedCount,
                     copyCount,
                     acc
             );
         }
    }

    public final Phase phase;
    public ArrayList<Node> nodes;
    public ArrayList<ErrorResponse> errorResponses;
    public Node current;
    public IReducer<I, R> reducer;

    public Accum(Phase phase, IReducer<I, R> reducer) {
        this.phase = phase;
        this.reducer = reducer;
        nodes = new ArrayList<>();
        errorResponses = new ArrayList<>();
        addNode();
    }

    public ArrayList<R> getResults () {
        final ArrayList<R> results = new ArrayList<>();
        for (Node subRes: nodes) {
            if (subRes.commandComplete != null) {
                R result = reducer.finalize(subRes.acc);
                results.add(result);
            }
        }
        return results;
    }

    public R getResult () {
        if (nodes.isEmpty()) {
            return null;
        }
        else {
            Node subRes = nodes.get(0);
            if (subRes.commandComplete != null) {
                return reducer.finalize(subRes.acc);
            }
            else {
                return null;
            }
        }
    }

    public void setCurrentValues (Object[] values) {
        current.acc = reducer.append(current.acc, current.keys, values);
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
