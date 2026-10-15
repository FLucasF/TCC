package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("2");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return PERCENTUAL_CREDITO;
    }

    @Override
    public BigDecimal frete(BigDecimal freteModalidade) {
        return freteModalidade;
    }

    @Override
    public boolean daBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
