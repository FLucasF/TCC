package br.com.marealta.store;

import java.util.function.Function;

/** Acesso aos dados: tudo o que roda dentro de {@link #transaction} é atômico. */
public interface Store {
    <T> T transaction(Function<Tx, T> work);
}
