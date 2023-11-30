package com.github.igrishaev;

public interface IReducer<InitType, FinalType> {
    InitType initiate();
    InitType append(InitType acc, Object row);
    FinalType finalize(InitType acc);
}
