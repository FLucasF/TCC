package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean elegivelBrinde(BigDecimal subtotalProdutos);
}
