package com.loja.checkout.web;

import com.loja.checkout.dominio.ErroCheckout;

/** O corpo devolvido quando o pedido é recusado. */
public record ErroResponse(String erro) {

    public static ErroResponse de(ErroCheckout erro) {
        return new ErroResponse(erro.name());
    }
}
