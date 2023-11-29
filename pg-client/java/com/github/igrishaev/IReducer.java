package com.github.igrishaev;

public interface IReducer<InitType, FinalType> {

    public InitType initiate();

    public InitType append(InitType acc, Object row);

    public FinalType finalize(InitType acc);

}
