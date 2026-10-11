package com.loja.checkout.comum;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agrupa implementacoes que variam por caso (uma modalidade de entrega, um
 * cupom, um nivel de clube, uma forma de pagamento) por sua chave, para que
 * escolher entre elas seja uma busca no mapa e nao uma sequencia de
 * condicoes. Novas implementacoes entram so adicionando um bean novo.
 */
public final class RegistroPorChave<K, V> {

    private final Map<K, V> porChave;

    public RegistroPorChave(List<V> valores, Function<V, K> chaveDe) {
        this.porChave = valores.stream().collect(Collectors.toMap(chaveDe, Function.identity()));
    }

    public Optional<V> buscar(K chave) {
        return Optional.ofNullable(porChave.get(chave));
    }
}
