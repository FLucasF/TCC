package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Escolhe uma opcao pelo codigo que o site enviou. Cadastrar uma opcao nova e
 * so acrescentar uma implementacao; nada aqui muda.
 */
public class Catalogo<T extends Identificavel> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Catalogo(Collection<? extends T> opcoes) {
        opcoes.forEach(opcao -> porCodigo.put(opcao.codigo(), opcao));
    }

    public Optional<T> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
