package com.loja.checkout.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NivelOuro implements NivelClube {

    private static final BigDecimal VALOR_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(new BigDecimal("0.05"));
        return credito.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal ajustarFrete(BigDecimal frete) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(VALOR_MINIMO_BRINDE) > 0;
    }
}
