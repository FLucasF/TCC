package com.loja.checkout.dominio;

import java.util.Optional;

/** Leitura dos codigos que o site envia: cada um tem o seu proprio erro. */
public final class Codigos {

    private Codigos() {
    }

    public static <E extends Enum<E>> E obrigatorio(Class<E> tipo, String codigo, ErroCheckout erro) {
        return opcional(tipo, codigo, erro).orElseThrow(() -> new CheckoutInvalidoException(erro));
    }

    public static <E extends Enum<E>> Optional<E> opcional(Class<E> tipo, String codigo, ErroCheckout erro) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(tipo, codigo));
        } catch (IllegalArgumentException naoExiste) {
            throw new CheckoutInvalidoException(erro);
        }
    }
}
