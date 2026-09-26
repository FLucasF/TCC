package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Guarda as opcoes disponiveis e acha a escolhida pelo codigo, sem cadeia de condicoes. */
public class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(
                Codificado::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
