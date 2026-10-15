package com.loja.checkout.domain.modalidade;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import java.util.Map;

public class ModalidadeFactory {
    private static final Map<String, ModalidadeEntrega> MODALIDADES = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    public static ModalidadeEntrega criar(String codigo) {
        if (codigo == null || !MODALIDADES.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return MODALIDADES.get(codigo);
    }
}
