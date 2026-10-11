package br.com.loja.checkout.dominio;

import java.util.Arrays;

public final class Codigos {

    private Codigos() {
    }

    public static <E extends Enum<E>> E obrigatorio(Class<E> tipo, String codigo, CodigoErro erro) {
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new CheckoutException(erro));
    }
}
