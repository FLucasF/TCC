package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements CalculadoraPagamento {
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MINIMAS = 1;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");

    @Override
    public boolean podeAtender(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= PARCELAS_MINIMAS && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = calcularTotalFinal(totalPedido, parcelas);
        return totalFinal.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido;
        }

        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        return valorParcela.multiply(BigDecimal.valueOf(parcelas));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        }

        BigDecimal um = BigDecimal.ONE;
        BigDecimal umMaisTaxa = um.add(TAXA_JUROS_MENSAL);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = um.subtract(um.divide(potencia, 10, RoundingMode.HALF_EVEN));

        return totalPedido
                .multiply(TAXA_JUROS_MENSAL)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
    }
}
