package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 5% em credito, frete gratis sempre e brinde acima de R$ 500,00 em produtos. */
@Component
public class ClubeOuro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("5");
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
