package com.loja.checkout.comum;

import java.util.Arrays;
import java.util.Optional;

/** Escolha do caso pelo código recebido, sem cadeia de condições. */
public final class Enums {

    private Enums() {
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
