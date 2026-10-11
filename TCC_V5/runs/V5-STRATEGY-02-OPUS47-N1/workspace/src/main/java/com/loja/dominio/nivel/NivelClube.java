package com.loja.dominio.nivel;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal taxaCredito();
    boolean freteGratis();
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
