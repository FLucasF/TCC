package com.loja.checkout.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolve um caso pelo seu código. Caso novo é bean novo: nada aqui muda, e a
 * escolha continua sendo uma busca em mapa, não uma cadeia de condições.
 */
public class Catalogo<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> casos) {
        this.porCodigo = casos.stream()
                .collect(Collectors.toUnmodifiableMap(Codificavel::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
