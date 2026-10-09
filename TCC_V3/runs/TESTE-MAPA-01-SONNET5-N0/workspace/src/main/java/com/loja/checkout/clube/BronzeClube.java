package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BronzeClube implements ClubeStrategy {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public boolean fretesGratis() {
        return false;
    }

    @Override
    public BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
