package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bronze implements Clube {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
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
