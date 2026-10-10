package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Prata implements Clube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String getCodigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO);
    }

    @Override
    public boolean isentaFrete() {
        return false;
    }

    @Override
    public boolean concedeBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
