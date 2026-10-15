package com.loja.checkout.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;

public class PagamentoCartao implements PagamentoStrategy {

    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return new BigDecimal("0.00");
        }

        BigDecimal valorComJuros = calcularTotalComJuros(total, parcelas);
        return valorComJuros.subtract(total);
    }

    private BigDecimal calcularTotalComJuros(BigDecimal total, Integer parcelas) {
        BigDecimal um = new BigDecimal("1");
        BigDecimal taxa = TAXA_JUROS_MENSAL;
        BigDecimal n = new BigDecimal(parcelas);

        MathContext mc = new MathContext(64, RoundingMode.HALF_EVEN);
        BigDecimal umMaisTaxa = um.add(taxa, mc);
        BigDecimal expoente = umMaisTaxa.pow(-parcelas.intValue(), mc);
        BigDecimal denominador = um.subtract(expoente, mc);

        BigDecimal parcelaBruta = total
            .multiply(taxa, mc)
            .divide(denominador, 10, RoundingMode.HALF_EVEN);

        BigDecimal parcela = parcelaBruta.setScale(2, RoundingMode.HALF_EVEN);
        return parcela.multiply(n);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal valorFinal, Integer parcelas) {
        BigDecimal parcela = valorFinal.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
        return parcela;
    }

    @Override
    public Integer getParcelasMaximas() {
        return 12;
    }

    @Override
    public boolean isDisponivel(BigDecimal total) {
        return true;
    }
}
