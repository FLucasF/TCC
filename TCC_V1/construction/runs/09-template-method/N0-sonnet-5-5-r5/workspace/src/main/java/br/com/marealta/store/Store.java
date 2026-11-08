package br.com.marealta.store;

import java.util.function.Function;

/** Executa trabalho de forma transacional. */
public interface Store {
    <T> T tx(Function<Tx, T> work);
}
