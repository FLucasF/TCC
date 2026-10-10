package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.util.Map;

public interface NivelClube {

    BigDecimal creditoPercentual();

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);

    Map<String, NivelClube> REGISTRO = Map.of(
            "BRONZE", new Bronze(),
            "PRATA", new Prata(),
            "OURO", new Ouro()
    );

    static NivelClube buscar(String codigo) {
        return codigo == null ? null : REGISTRO.get(codigo);
    }
}
