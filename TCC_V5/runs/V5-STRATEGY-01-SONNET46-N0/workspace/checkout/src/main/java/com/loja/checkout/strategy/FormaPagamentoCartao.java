package com.loja.checkout.strategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class FormaPagamentoCartao implements FormaPagamento {
    private static final BigDecimal TAXA_MENSAL = BigDecimal.valueOf(0.0199);

    @Override public String codigo() { return "CARTAO"; }
    @Override public boolean aceitaParcelas(int parcelas) { return parcelas >= 1 && parcelas <= 12; }
    @Override public boolean aceita(BigDecimal totalPedido, int parcelas) { return true; }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return calcularTotalFinal(totalPedido, parcelas).subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido;
        }
        BigDecimal parcela = calcularValorParcela(totalPedido, parcelas);
        return parcela.multiply(BigDecimal.valueOf(parcelas));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        }
        // Price formula: parcela = total * taxa / (1 - (1 + taxa)^(-n))
        BigDecimal taxa = TAXA_MENSAL;
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        // (1 + taxa)^(-n) = 1 / (1 + taxa)^n
        BigDecimal fator = umMaisTaxa.pow(parcelas, new MathContext(20, RoundingMode.HALF_EVEN));
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, 20, RoundingMode.HALF_EVEN));
        BigDecimal parcela = totalPedido.multiply(taxa).divide(denominador, 2, RoundingMode.HALF_EVEN);
        return parcela;
    }
}
