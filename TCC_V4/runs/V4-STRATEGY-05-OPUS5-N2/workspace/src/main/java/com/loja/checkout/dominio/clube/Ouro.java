package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/**
 * Ganha 5% dos produtos de volta em credito, nao paga frete nunca e, acima de
 * R$ 500,00 em produtos, recebe um brinde junto.
 */
public final class Ouro implements Clube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("5");
    private static final BigDecimal PRODUTOS_PARA_BRINDE = new BigDecimal("500.00");

    @Override
    public Vantagens vantagens(BigDecimal subtotalProdutos, BigDecimal freteCalculado) {
        return new Vantagens(
                Dinheiro.percentual(PERCENTUAL_CREDITO, subtotalProdutos),
                Dinheiro.ZERO,
                subtotalProdutos.compareTo(PRODUTOS_PARA_BRINDE) > 0);
    }
}
