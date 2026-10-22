package br.com.mealta.store;

import java.util.function.Supplier;

/** Executa o bloco inteiro como uma operação só: ou tudo é gravado, ou nada. */
public interface Transactor {
    <T> T run(Supplier<T> work);
}
