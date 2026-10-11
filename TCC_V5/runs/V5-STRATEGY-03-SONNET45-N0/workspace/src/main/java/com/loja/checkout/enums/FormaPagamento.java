package com.loja.checkout.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX(1, 1, null, new BigDecimal("-0.05"), null),
    BOLETO(1, 1, new BigDecimal("3.49"), null, new BigDecimal("1000.00")),
    CARTAO(1, 12, null, null, null);

    private final int parcelasMinimas;
    private final int parcelasMaximas;
    private final BigDecimal tarifa;
    private final BigDecimal percentualDesconto;
    private final BigDecimal limiteValor;

    FormaPagamento(int parcelasMinimas, int parcelasMaximas, BigDecimal tarifa,
                   BigDecimal percentualDesconto, BigDecimal limiteValor) {
        this.parcelasMinimas = parcelasMinimas;
        this.parcelasMaximas = parcelasMaximas;
        this.tarifa = tarifa;
        this.percentualDesconto = percentualDesconto;
        this.limiteValor = limiteValor;
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= parcelasMinimas && parcelas <= parcelasMaximas;
    }

    public boolean aceitaValor(BigDecimal valor) {
        return limiteValor == null || valor.compareTo(limiteValor) <= 0;
    }

    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (this == PIX) {
            BigDecimal desconto = totalPedido.multiply(percentualDesconto);
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }

        if (this == BOLETO) {
            return tarifa;
        }

        if (this == CARTAO) {
            if (parcelas <= 3) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            }

            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal potencia = umMaisTaxa.pow(parcelas);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN));
            BigDecimal valorParcela = totalPedido.multiply(taxa).divide(denominador, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalComJuros = valorParcela.multiply(new BigDecimal(parcelas));

            return totalComJuros.subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);
        }

        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        BigDecimal valorParcela = totalFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        return valorParcela;
    }
}
