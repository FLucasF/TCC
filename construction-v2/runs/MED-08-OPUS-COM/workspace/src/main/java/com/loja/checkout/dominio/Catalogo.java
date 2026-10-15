package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Escolhe uma opcao pelo codigo recebido, sem encadear condicoes. */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Catalogo(Collection<T> opcoes, Function<T, String> codigo) {
        opcoes.forEach(opcao -> porCodigo.put(codigo.apply(opcao), opcao));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
