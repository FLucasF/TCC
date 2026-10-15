package com.loja.checkout.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Catalogo de opcoes (entregas, cupons, niveis, formas de pagamento) indexado
 * pelo codigo que chega na requisicao. Cada opcao nova registrada no Spring
 * entra aqui sozinha.
 */
public class Registro<T> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Registro(Iterable<T> opcoes, Function<T, String> codigo) {
        for (T opcao : opcoes) {
            porCodigo.put(codigo.apply(opcao), opcao);
        }
    }

    /** Busca pelo codigo exato (maiusculas, como combinado com o site). */
    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }

    public Map<String, T> opcoes() {
        return Map.copyOf(porCodigo);
    }
}
