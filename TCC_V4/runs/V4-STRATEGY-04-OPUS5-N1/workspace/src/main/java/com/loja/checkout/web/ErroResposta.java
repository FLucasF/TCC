package com.loja.checkout.web;

import com.loja.checkout.dominio.CodigoErro;

/** O servico recusou o pedido: devolve so o codigo do problema. */
public record ErroResposta(String erro) {

    public static ErroResposta de(CodigoErro codigo) {
        return new ErroResposta(codigo.name());
    }
}
