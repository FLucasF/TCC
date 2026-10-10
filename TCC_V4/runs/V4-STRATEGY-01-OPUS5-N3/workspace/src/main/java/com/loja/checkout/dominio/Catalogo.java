package com.loja.checkout.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalogo de casos de um eixo de variacao: traduz o codigo recebido do site
 * na instancia que carrega a regra daquele caso. A escolha do caso e sempre
 * uma busca aqui, nunca uma cadeia de condicoes.
 */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;

    private Catalogo(Map<String, T> porCodigo) {
        this.porCodigo = porCodigo;
    }

    /** Dois casos com o mesmo codigo sao erro de cadastro, nao sobrescrita silenciosa. */
    public static <T> Catalogo<T> de(List<T> casos, Function<T, String> codigo) {
        return new Catalogo<>(casos.stream()
                .collect(Collectors.toUnmodifiableMap(codigo, caso -> caso)));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
