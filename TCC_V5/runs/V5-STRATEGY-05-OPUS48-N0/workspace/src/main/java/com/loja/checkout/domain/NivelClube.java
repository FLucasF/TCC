package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Níveis do clube da loja e as vantagens de cada um.
 *
 * <p>Estamos estudando criar mais níveis: para acrescentar um, basta adicionar um
 * novo valor aqui com seu percentual de crédito, se paga ou não frete, e o valor de
 * produtos a partir do qual acompanha brinde ({@code null} = nunca manda brinde).
 *
 * <ul>
 *   <li>O crédito é sobre o valor dos produtos (sem desconto e sem frete), guardado
 *       para a próxima compra — não abate nada nesta.</li>
 *   <li>Quando o nível não paga frete, o frete sai zerado no resumo.</li>
 * </ul>
 */
public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, null),
    PRATA(new BigDecimal("0.02"), false, null),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal brindeAcimaDe;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, BigDecimal brindeAcimaDe) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.brindeAcimaDe = brindeAcimaDe;
    }

    /** Crédito para a próxima compra, sobre o valor dos produtos, arredondado para centavos. */
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualCredito);
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    /** Indica se o pedido ganha brinde, dado o valor dos produtos. */
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return brindeAcimaDe != null && subtotalProdutos.compareTo(brindeAcimaDe) > 0;
    }
}
