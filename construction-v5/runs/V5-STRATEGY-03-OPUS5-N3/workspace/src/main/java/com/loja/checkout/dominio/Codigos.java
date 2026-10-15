package com.loja.checkout.dominio;

import java.util.Arrays;
import java.util.Optional;

/** Resolve o código enviado pelo site num caso que é sempre o mesmo conjunto fechado. */
public final class Codigos {

    private Codigos() {
    }

    public static <E extends Enum<E>> Optional<E> buscar(Class<E> tipo, String codigo) {
        return Arrays.stream(tipo.getEnumConstants())
                .filter(caso -> caso.name().equals(codigo))
                .findFirst();
    }
}
