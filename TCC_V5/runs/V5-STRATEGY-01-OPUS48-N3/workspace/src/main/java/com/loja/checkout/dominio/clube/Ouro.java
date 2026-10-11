package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(CREDITO);
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
