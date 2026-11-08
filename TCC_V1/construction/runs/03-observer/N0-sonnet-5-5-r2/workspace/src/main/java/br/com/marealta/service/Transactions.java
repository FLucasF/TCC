package br.com.marealta.service;

import java.util.function.Supplier;

/** Executa o trabalho de forma atômica: tudo é gravado, ou nada é. */
public interface Transactions {
    <T> T run(Supplier<T> work);
}
