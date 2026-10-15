package com.loja.strategy.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class PagamentoCartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return new BigDecimal("0.00");
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return totalFinal.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128);
        BigDecimal umMenosDenominador = BigDecimal.ONE.subtract(denominador);

        BigDecimal numerador = totalPedido.multiply(TAXA_JUROS);
        BigDecimal valorParcela = numerador.divide(umMenosDenominador, 2, RoundingMode.HALF_EVEN);

        return valorParcela;
    }
}
