package com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Catalogo de opcoes identificadas por codigo (entregas, cupons, niveis, pagamentos).
 * Basta publicar um novo bean da interface correspondente para a opcao passar a existir.
 */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;

    public Catalogo(Iterable<T> opcoes, Function<T, String> codigo) {
        Map<String, T> mapa = new LinkedHashMap<>();
        for (T opcao : opcoes) {
            T anterior = mapa.put(codigo.apply(opcao), opcao);
            if (anterior != null) {
                throw new IllegalStateException("Codigo duplicado: " + codigo.apply(opcao));
            }
        }
        this.porCodigo = Map.copyOf(mapa);
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }

    /** Busca o codigo ou recusa o pedido com o erro informado. */
    public T exigir(String codigo, ErroCheckout erro) {
        return buscar(codigo).orElseThrow(() -> new CheckoutException(erro));
    }
}
