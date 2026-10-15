package com.loja.resumo.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NivelOuro implements NivelClube {

    private static final BigDecimal BRINDE_ACIMA_DE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("0.05");
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(BRINDE_ACIMA_DE) > 0;
    }
}
