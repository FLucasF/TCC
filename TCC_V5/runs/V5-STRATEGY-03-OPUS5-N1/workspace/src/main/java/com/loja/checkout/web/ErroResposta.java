package com.loja.checkout.web;

import com.loja.checkout.dominio.CodigoErro;

public record ErroResposta(String erro) {

    public ErroResposta(CodigoErro codigo) {
        this(codigo.name());
    }
}
