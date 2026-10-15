package com.loja.checkout.dominio;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Escolhe uma opcao pelo codigo, sem cadeia de condicoes. */
public class Catalogo<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    protected Catalogo(Iterable<T> opcoes) {
        this.porCodigo = java.util.stream.StreamSupport.stream(opcoes.spliterator(), false)
                .collect(Collectors.toMap(Codificavel::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
