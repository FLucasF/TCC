package com.loja.service.estrategia;

import com.loja.util.Arredondador;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoCartao implements CalculoPagamento {
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private Integer parcelas;

    public PagamentoCartao(Integer parcelas) {
        this.parcelas = parcelas != null ? parcelas : 1;
    }

    @Override
    public BigDecimal calcularParcela(BigDecimal total, Integer numParcelas) {
        if (numParcelas <= 3) {
            BigDecimal parcela = total.divide(new BigDecimal(numParcelas), 10, RoundingMode.HALF_EVEN);
            return Arredondador.arredondarParaCentavos(parcela);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
        BigDecimal expoente = umMaisTaxa.pow(numParcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(expoente, 10, RoundingMode.HALF_EVEN));
        BigDecimal parcela = total.multiply(TAXA_JUROS_MENSAL).divide(denominador, 10, RoundingMode.HALF_EVEN);

        return Arredondador.arredondarParaCentavos(parcela);
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal parcelaCalculada, Integer numParcelas, BigDecimal totalAntesAjuste) {
        BigDecimal totalFinal = parcelaCalculada.multiply(new BigDecimal(numParcelas));
        BigDecimal ajuste = totalFinal.subtract(totalAntesAjuste);
        return Arredondador.arredondarParaCentavos(ajuste);
    }

    @Override
    public boolean validarParcelas(Integer parcelas) {
        if (parcelas == null) {
            return true;
        }
        return parcelas >= 1 && parcelas <= 12;
    }
}
