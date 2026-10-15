package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.BeneficiosClube;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ClubeOuro implements BeneficiosClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
