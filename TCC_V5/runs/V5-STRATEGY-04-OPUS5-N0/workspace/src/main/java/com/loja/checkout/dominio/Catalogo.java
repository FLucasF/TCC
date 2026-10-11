package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lista de opções disponíveis, indexada pelo código. Para acrescentar uma opção nova
 * (uma transportadora, um cupom, um nível do clube) basta registrar mais uma implementação:
 * ela entra no catálogo sozinha, sem mexer no cálculo.
 */
public class Catalogo<T extends Identificavel> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Catalogo(Collection<T> opcoes) {
        for (T opcao : opcoes) {
            porCodigo.put(opcao.codigo(), opcao);
        }
    }

    /** Busca pelo código exato (maiúsculas). Vazio quando o código não existe ou não veio. */
    public Optional<T> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }

    public Collection<T> opcoes() {
        return porCodigo.values();
    }
}
