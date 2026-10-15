package com.loja.checkout.model;

import java.math.BigDecimal;

public enum FormaPagamento {
    PIX(1, 1),
    CARTAO(1, 12),
    BOLETO(1, 1);

    private final Integer parcelasMinimas;
    private final Integer parcelasMaximas;

    FormaPagamento(Integer parcelasMinimas, Integer parcelasMaximas) {
        this.parcelasMinimas = parcelasMinimas;
        this.parcelasMaximas = parcelasMaximas;
    }

    public Integer getParcelasMinimas() {
        return parcelasMinimas;
    }

    public Integer getParcelasMaximas() {
        return parcelasMaximas;
    }

    public boolean ehParcelamentoValido(Integer parcelas) {
        return parcelas >= parcelasMinimas && parcelas <= parcelasMaximas;
    }

    public BigDecimal getTaxaMensal() {
        if (this == CARTAO) {
            return new BigDecimal("0.0199");
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        if (this == PIX) {
            return total.multiply(new BigDecimal("0.05")).negate();
        }
        if (this == BOLETO) {
            return new BigDecimal("3.49");
        }
        if (this == CARTAO) {
            if (parcelas <= 3) {
                return BigDecimal.ZERO;
            }
            java.math.MathContext mc = new java.math.MathContext(50);
            BigDecimal taxa = getTaxaMensal();
            BigDecimal taxaMais1 = BigDecimal.ONE.add(taxa);
            BigDecimal potencia = taxaMais1.pow(parcelas, mc);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, mc));
            BigDecimal parcelaComMuitaPrecisao = total.multiply(taxa, mc).divide(divisor, mc);
            BigDecimal totalComJuros = parcelaComMuitaPrecisao.multiply(BigDecimal.valueOf(parcelas), mc);
            return totalComJuros.subtract(total);
        }
        return BigDecimal.ZERO;
    }
}
