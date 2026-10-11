package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class Catalogo<T extends Identificavel> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Catalogo(Collection<? extends T> itens) {
        itens.forEach(item -> porCodigo.put(item.codigo(), item));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
