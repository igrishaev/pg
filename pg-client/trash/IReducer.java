package com.github.igrishaev;

import clojure.lang.PersistentVector;
import clojure.lang.PersistentVector$TransientVector;


puclic interface IReducer<Foo, Bar> {

    Foo getInit();

    Foo addNext(Foo acc, Object row);

    Bar finalize(Foo acc);

}


public class VectorReducer implements IReducer<PersistentVector$TransientVector, PersistentVector> {

    PersistentVector$TransientVector getInit() {
        return PersistentVector.EMPTY().asTransient();
    }

    PersistentVector$TransientVector addNext(PersistentVector$TransientVector acc, Object row) {
        return PersistentVector$TransientVector.conj(acc, row);
    }

    PersistentVector finalize(PersistentVector$TransientVector acc) {
        return acc.persistent();
    }

}
