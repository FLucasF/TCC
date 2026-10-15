package com.loja.checkout.clube;

import com.loja.checkout.dominio.Registro;

/** Os niveis do clube da loja. */
public final class NiveisClube {

    public static final Registro<NivelClube> REGISTRO = new Registro<>(
            new Bronze(),
            new Prata(),
            new Ouro());

    private NiveisClube() {
    }
}
