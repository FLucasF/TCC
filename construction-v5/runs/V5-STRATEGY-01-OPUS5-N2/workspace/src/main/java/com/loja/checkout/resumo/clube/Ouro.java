package com.loja.checkout.resumo.clube;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** Ganha 5% dos produtos em crédito, não paga frete nunca e, acima de R$ 500,00 em produtos, leva brinde. */
public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public BigDecimal frete(BigDecimal freteCalculado) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
