package com.loja.resumo.clube;

import com.loja.resumo.Codigado;
import java.math.BigDecimal;

public interface NivelClube extends Codigado {

    BigDecimal percentualCredito();

    default boolean freteGratis() {
        return false;
    }

    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
