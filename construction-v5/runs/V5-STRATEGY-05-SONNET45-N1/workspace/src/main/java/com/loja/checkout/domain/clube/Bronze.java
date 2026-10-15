package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.NivelClube;
import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return new BigDecimal("0.00");
    }

    @Override
    public boolean freteSempre​Gratis() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
