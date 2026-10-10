package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.BeneficiosClube;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ClubePrata implements BeneficiosClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.02"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
