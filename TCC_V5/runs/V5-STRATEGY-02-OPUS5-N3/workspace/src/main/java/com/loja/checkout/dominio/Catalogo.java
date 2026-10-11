package com.loja.checkout.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Escolhe um caso pelo código que o site mandou. Nunca lança: quem chama
 * decide qual erro o código desconhecido representa.
 */
public abstract class Catalogo<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    protected Catalogo(List<T> opcoes) {
        this.porCodigo = opcoes.stream()
                .collect(Collectors.toUnmodifiableMap(Codificavel::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
