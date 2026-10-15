package com.loja.checkout.calculo;

import java.util.Arrays;

final class Codigos {

    private Codigos() {
    }

    static <E extends Enum<E>> E buscar(E[] valores, String codigo, Erro erroSeNaoExiste) {
        return Arrays.stream(valores)
                .filter(valor -> valor.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ResumoException(erroSeNaoExiste));
    }
}
