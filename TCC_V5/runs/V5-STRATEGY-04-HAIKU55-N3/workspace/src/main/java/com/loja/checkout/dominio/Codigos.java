package com.loja.checkout.dominio;

import java.util.Arrays;
import java.util.Optional;

public final class Codigos {

    private Codigos() {
    }

    public static <E extends Enum<E>> Optional<E> buscar(Class<E> tipo, String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equals(codigo))
                .findFirst();
    }
}
