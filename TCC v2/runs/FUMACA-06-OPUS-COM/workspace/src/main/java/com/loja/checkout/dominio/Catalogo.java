package com.loja.checkout.dominio;

import java.util.Arrays;

/** Traduz o codigo que chega na requisicao para a opcao correspondente. */
final class Catalogo {

    private Catalogo() {
    }

    static <E extends Enum<E>> E resolver(Class<E> opcoes, String codigo, CodigoErro erro) {
        return Arrays.stream(opcoes.getEnumConstants())
                .filter(opcao -> opcao.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ErroCheckout(erro));
    }
}
