package com.loja.checkout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Identificavel {
    String codigo();

    static <T extends Identificavel> Map<String, T> indexar(List<T> opcoes) {
        Map<String, T> mapa = new HashMap<>();
        opcoes.forEach(o -> mapa.put(o.codigo(), o));
        return mapa;
    }
}
