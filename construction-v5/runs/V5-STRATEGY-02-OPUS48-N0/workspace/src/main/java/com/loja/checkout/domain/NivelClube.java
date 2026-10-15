package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Cada nivel define quanto de credito volta
 * para a proxima compra, se paga frete e a partir de quanto ganha brinde.
 * Para criar um nivel novo, basta adicionar uma constante aqui.
 */
public enum NivelClube {

    BRONZE("0", false, null),
    PRATA("0.02", false, null),
    OURO("0.05", true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal brindeAcimaDe;

    NivelClube(String percentualCredito, boolean freteGratis, BigDecimal brindeAcimaDe) {
        this.percentualCredito = new BigDecimal(percentualCredito);
        this.freteGratis = freteGratis;
        this.brindeAcimaDe = brindeAcimaDe;
    }

    /** Este nivel nunca paga frete? (OURO). */
    public boolean temFreteGratis() {
        return freteGratis;
    }

    /** Credito para a proxima compra: percentual sobre o valor dos produtos. */
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(percentualCredito));
    }

    /** Ganha brinde? So se o nivel der brinde e os produtos passarem do limite. */
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return brindeAcimaDe != null && subtotalProdutos.compareTo(brindeAcimaDe) > 0;
    }

    /** Converte o texto do site; lanca NIVEL_CLUBE_INVALIDO se nao existir. */
    public static NivelClube de(String valor) {
        if (valor != null) {
            for (NivelClube nivel : values()) {
                if (nivel.name().equals(valor)) {
                    return nivel;
                }
            }
        }
        throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }
}
