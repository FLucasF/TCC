package com.loja.checkout.service.clube;

import com.loja.checkout.enums.NivelClube;

import java.math.BigDecimal;

public interface BeneficioClube {

    NivelClube getNivel();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
