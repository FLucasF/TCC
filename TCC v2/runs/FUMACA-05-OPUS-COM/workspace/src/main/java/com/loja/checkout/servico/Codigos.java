package com.loja.checkout.servico;

import java.util.Arrays;
import java.util.Optional;

final class Codigos {

    private Codigos() {
    }

    /** O codigo vem sempre em maiusculas, exatamente como cadastrado. */
    static <E extends Enum<E>> Optional<E> procurar(Class<E> tipo, String codigo) {
        return Optional.ofNullable(codigo)
                .flatMap(informado -> Arrays.stream(tipo.getEnumConstants())
                        .filter(constante -> constante.name().equals(informado))
                        .findFirst());
    }
}
