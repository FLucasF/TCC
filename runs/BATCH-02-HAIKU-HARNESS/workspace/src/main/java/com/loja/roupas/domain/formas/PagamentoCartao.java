package com.loja.roupas.domain.formas;

import com.loja.roupas.domain.FormaPagamento;
import com.loja.roupas.domain.util.Arredondador;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoCartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public boolean estaDisponivel(Integer parcelas, BigDecimal total) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO;
        }

        BigDecimal valorParcela = calcularValorParcelaComJuros(total, parcelas);
        BigDecimal valorTotal = valorParcela.multiply(new BigDecimal(parcelas));
        return valorTotal.subtract(total);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, Integer parcelas) {
        return Arredondador.arredondar(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
    }

    private BigDecimal calcularValorParcelaComJuros(BigDecimal total, Integer parcelas) {
        BigDecimal taxa = TAXA_JUROS;
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);

        BigDecimal potencia = umMaisTaxa.pow(parcelas, java.math.MathContext.DECIMAL128);
        BigDecimal inversa = BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(inversa);

        BigDecimal numerador = total.multiply(taxa);
        BigDecimal parcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);
        return Arredondador.arredondar(parcela);
    }
}
