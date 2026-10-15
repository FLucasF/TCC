package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalogo de opcoes disponiveis no sistema (entregas, cupons, niveis do clube...).
 * Para acrescentar uma opcao nova basta registrar um bean novo: ele entra aqui sozinho.
 */
public class Catalogo<T extends Identificavel> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(
                T::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
