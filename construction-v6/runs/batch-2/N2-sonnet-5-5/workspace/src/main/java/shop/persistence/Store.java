package shop.persistence;

import java.util.function.Function;

/** Fronteira de transação: tudo o que roda dentro de {@code work} é confirmado junto ou desfeito junto. */
public interface Store {
    <T> T inTransaction(Function<Tx, T> work);
}
