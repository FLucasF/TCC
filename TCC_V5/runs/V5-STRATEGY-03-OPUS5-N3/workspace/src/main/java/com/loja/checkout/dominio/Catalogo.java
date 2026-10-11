package com.loja.checkout.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolve o código enviado pelo site no caso correspondente. A escolha do caso é um
 * lookup: um caso novo é uma classe nova, não um ramo novo de condição.
 */
public final class Catalogo<T extends Identificavel> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> casos) {
        this.porCodigo = casos.stream().collect(Collectors.toMap(Identificavel::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
