package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.CodigoErro;

public record ErroResponse(String erro) {

    public ErroResponse(CodigoErro codigo) {
        this(codigo.name());
    }
}
