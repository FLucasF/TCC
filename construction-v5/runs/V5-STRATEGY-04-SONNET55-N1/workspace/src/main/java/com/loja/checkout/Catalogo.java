package com.loja.checkout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Índice de implementações por código; a escolha é uma busca, não uma cadeia de condições. */
public class Catalogo<T extends Identificavel> {
    private final Map<String, T> porCodigo = new HashMap<>();

    public Catalogo(List<T> opcoes) {
        opcoes.forEach(o -> porCodigo.put(o.codigo(), o));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
