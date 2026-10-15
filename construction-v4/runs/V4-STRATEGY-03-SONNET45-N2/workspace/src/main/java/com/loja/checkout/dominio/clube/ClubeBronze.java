package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.BeneficiosClube;
import java.math.BigDecimal;

public class ClubeBronze implements BeneficiosClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO.setScale(2);
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
