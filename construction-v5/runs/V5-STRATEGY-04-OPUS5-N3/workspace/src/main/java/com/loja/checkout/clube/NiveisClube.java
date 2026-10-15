package com.loja.checkout.clube;

import com.loja.checkout.dominio.Registro;

import java.util.List;
import java.util.Optional;

/** Os níveis do clube existentes hoje. */
public final class NiveisClube {

    private static final List<NivelClube> CASOS = List.of(new Bronze(), new Prata(), new Ouro());

    private NiveisClube() {
    }

    public static Optional<NivelClube> porCodigo(String codigo) {
        return Registro.busca(CASOS, NivelClube::codigo, codigo);
    }
}
