package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal LIMIAR_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public boolean isFreteGratis() {
        return true;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMIAR_BRINDE) > 0;
    }
}
