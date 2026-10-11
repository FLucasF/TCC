package com.loja.checkout.dominio;

import java.util.Arrays;
import java.util.Optional;

public final class Busca {

    private Busca() {
    }

    public static <E extends Enum<E>> Optional<E> porNome(Class<E> tipo, String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equals(nome))
                .findFirst();
    }
}
