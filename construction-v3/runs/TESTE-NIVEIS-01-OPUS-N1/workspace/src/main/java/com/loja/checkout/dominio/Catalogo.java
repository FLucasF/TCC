package com.loja.checkout.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarda as opcoes de um tipo e acha a opcao pelo codigo que o site mandou.
 * Uma opcao nova entra no catalogo so por existir; nada aqui muda.
 */
public class Catalogo<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> opcoes) {
        this.porCodigo = opcoes.stream()
                .collect(Collectors.toUnmodifiableMap(Codificavel::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
