package com.loja.checkout.dominio;

/** Traducao de codigo recebido do site para a opcao cadastrada no sistema. */
public final class Catalogo {

    private Catalogo() {
    }

    public static <E extends Enum<E>> E exigir(Class<E> opcoes, String codigo, ErroCheckout erro) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(erro);
        }
        try {
            return Enum.valueOf(opcoes, codigo);
        } catch (IllegalArgumentException naoCadastrado) {
            throw new CheckoutException(erro);
        }
    }
}
