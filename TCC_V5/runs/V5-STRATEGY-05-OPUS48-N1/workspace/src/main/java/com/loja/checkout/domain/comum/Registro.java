package com.loja.checkout.domain.comum;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarda um conjunto de casos pelo seu codigo e acha o certo por busca.
 * Adicionar um caso novo (uma transportadora, um cupom, um nivel do clube) e
 * so criar a classe dele; nada aqui muda.
 */
public final class Registro<T extends ComCodigo> {

    private final Map<String, T> porCodigo;

    public Registro(List<T> casos) {
        this.porCodigo = casos.stream()
                .collect(Collectors.toMap(ComCodigo::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
