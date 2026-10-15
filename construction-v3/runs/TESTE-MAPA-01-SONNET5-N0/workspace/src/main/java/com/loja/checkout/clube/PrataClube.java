package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PrataClube implements ClubeStrategy {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public boolean fretesGratis() {
        return false;
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("0.02");
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
