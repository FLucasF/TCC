package br.com.marealta.store;

import java.util.function.Function;

public interface Store {
    /** Runs the work atomically: everything is saved, or nothing is. */
    <T> T inTransaction(Function<Tx, T> work);
}
