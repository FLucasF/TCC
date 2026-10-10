package com.loja.service.estrategia;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AjusteCartaoPagamento implements AjustePagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO;
        }

        BigDecimal parcelaComJuros = calcularParcelaPrice(total, parcelas);
        BigDecimal totalComJuros = parcelaComJuros.multiply(new BigDecimal(parcelas));
        return totalComJuros.subtract(total);
    }

    @Override
    public int obterParcelas(Integer parcelasRequest) {
        return parcelasRequest != null ? parcelasRequest : 1;
    }

    private BigDecimal calcularParcelaPrice(BigDecimal total, int parcelas) {
        BigDecimal taxa = TAXA_JUROS;
        BigDecimal um = BigDecimal.ONE;
        BigDecimal umMaisTaxa = um.add(taxa);

        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal numerador = total.multiply(taxa).multiply(potencia);
        BigDecimal denominador = potencia.subtract(um);

        return numerador.divide(denominador, 2, RoundingMode.HALF_EVEN);
    }
}
