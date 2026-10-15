package com.loja.checkout.api;

import com.loja.checkout.dominio.Erro;

/** A resposta de um pedido recusado: so o codigo do problema. */
public record ErroResponse(String erro) {

    public static ErroResponse de(Erro erro) {
        return new ErroResponse(erro.name());
    }
}
