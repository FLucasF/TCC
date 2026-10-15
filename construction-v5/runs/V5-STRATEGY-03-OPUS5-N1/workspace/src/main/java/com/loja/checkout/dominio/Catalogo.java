package com.loja.checkout.dominio;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Busca uma opção pelo código que o site envia, sem encadear condições. */
public final class Catalogo<T extends Enum<T>> {

    private final Map<String, T> porCodigo;

    private Catalogo(Map<String, T> porCodigo) {
        this.porCodigo = porCodigo;
    }

    public static <T extends Enum<T>> Catalogo<T> de(Class<T> tipo) {
        return new Catalogo<>(Arrays.stream(tipo.getEnumConstants())
                .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity())));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
