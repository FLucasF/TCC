package com.loja.checkout.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Resolução de um código (texto do site) para o membro do enum. Lookup por
 * nome: código ausente ou desconhecido vira Optional vazio, e quem chama
 * decide o código de erro. Evita cadeia de condições para escolher o caso.
 */
final class Catalogo {

    private Catalogo() {
    }

    static <E extends Enum<E>> Optional<E> achar(Class<E> tipo, String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Arrays.stream(tipo.getEnumConstants())
                .filter(e -> e.name().equals(codigo))
                .findFirst();
    }
}
