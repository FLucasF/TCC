package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 5% dos produtos em credito, nunca paga frete e ganha brinde acima de R$ 500,00 em produtos. */
@Component
public class ClubeOuro implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return CREDITO;
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
