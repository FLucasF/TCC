package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BeneficiosClube calcular(BigDecimal subtotalProdutos);
}
