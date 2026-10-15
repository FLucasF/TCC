package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
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
