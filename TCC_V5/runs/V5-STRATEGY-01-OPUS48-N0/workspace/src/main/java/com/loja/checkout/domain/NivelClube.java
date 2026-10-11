package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Cada nivel define:
 *  - a porcentagem dos produtos que volta como credito para a proxima compra;
 *  - se o cliente nao paga frete;
 *  - se ganha brinde quando os produtos passam de certo valor.
 *
 * Para criar um nivel novo basta acrescentar uma constante aqui com as suas
 * vantagens.
 */
public enum NivelClube {

    /** So o cadastro, nao ganha nada. */
    BRONZE("0", false, null),

    /** 2% dos produtos de volta em credito. */
    PRATA("0.02", false, null),

    /** 5% de credito, nao paga frete nunca e brinde acima de R$ 500,00 em produtos. */
    OURO("0.05", true, new BigDecimal("500.00"));

    private final BigDecimal aliquotaCredito;
    private final boolean freteGratis;
    private final BigDecimal limiteBrinde;

    NivelClube(String aliquotaCredito, boolean freteGratis, BigDecimal limiteBrinde) {
        this.aliquotaCredito = new BigDecimal(aliquotaCredito);
        this.freteGratis = freteGratis;
        this.limiteBrinde = limiteBrinde;
    }

    /** Credito = aliquota x subtotal dos produtos, arredondado para centavos. */
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Money.cents(subtotalProdutos.multiply(aliquotaCredito));
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    /** O cliente ganha brinde se o nivel da direito e os produtos passam do limite. */
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return limiteBrinde != null && subtotalProdutos.compareTo(limiteBrinde) > 0;
    }

    public static NivelClube fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (NivelClube n : values()) {
            if (n.name().equals(codigo)) {
                return n;
            }
        }
        return null;
    }
}
