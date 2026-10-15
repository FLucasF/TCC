package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface BeneficiosClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temFretGratis();
    boolean temBrindeAcima500();
}
