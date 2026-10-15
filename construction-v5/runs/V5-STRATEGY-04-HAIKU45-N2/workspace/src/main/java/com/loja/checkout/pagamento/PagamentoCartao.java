package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import com.loja.checkout.util.Arredonda;

public class PagamentoCartao implements CalculadoraPagamento {
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public PagamentoInfo calcular(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = Arredonda.arredonda(total.divide(new BigDecimal(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalFinal.subtract(total);

            return new PagamentoInfo(ajuste, parcelas, valorParcela);
        } else {
            BigDecimal taxa = TAXA_MENSAL;
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal potencia = umMaisTaxa.pow(parcelas, new java.math.MathContext(30));
            BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(potencia, 10, java.math.RoundingMode.HALF_EVEN)
            );
            BigDecimal parcela = total.multiply(taxa).divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
            parcela = Arredonda.arredonda(parcela);

            BigDecimal totalFinal = parcela.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalFinal.subtract(total);

            return new PagamentoInfo(ajuste, parcelas, parcela);
        }
    }

    @Override
    public boolean podeParcelar(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean podeUsar(BigDecimal total) {
        return true;
    }
}
