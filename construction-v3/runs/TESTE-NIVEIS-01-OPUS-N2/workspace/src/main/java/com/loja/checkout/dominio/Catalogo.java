package com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda as opcoes existentes de um tipo (entrega, cupom, clube, pagamento) e
 * resolve o codigo que o site enviou. Opcao nova entra no catalogo so por
 * existir: a escolha nunca vira uma sequencia de condicoes.
 */
public final class Catalogo<T extends Catalogavel> {

    private final Map<String, T> porCodigo;

    public Catalogo(Iterable<T> opcoes) {
        Map<String, T> mapa = new LinkedHashMap<>();
        opcoes.forEach(opcao -> mapa.put(opcao.codigo(), opcao));
        this.porCodigo = Map.copyOf(mapa);
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
