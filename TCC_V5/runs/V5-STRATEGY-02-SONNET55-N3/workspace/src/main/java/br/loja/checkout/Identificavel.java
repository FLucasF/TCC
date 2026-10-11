package br.loja.checkout;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Algo escolhido pelo código recebido do site. */
public interface Identificavel {
    String codigo();

    static <T extends Identificavel> Map<String, T> indexar(List<T> opcoes) {
        return opcoes.stream().collect(Collectors.toUnmodifiableMap(Identificavel::codigo, Function.identity()));
    }
}
