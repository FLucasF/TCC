package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements Clube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal SUBTOTAL_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO);
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean concedeBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_MINIMO_BRINDE) > 0;
    }
}
