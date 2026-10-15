package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.CalculadoraPagamento;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoCartao implements CalculadoraPagamento {

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean aceitaTotal(BigDecimal total) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = calcularTotalFinal(total, parcelas);
        return totalFinal.subtract(total);
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return total;
        }

        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(umMaisTaxaElevado, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal valorParcela = total.multiply(taxa)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);

        return valorParcela.multiply(BigDecimal.valueOf(parcelas));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
    }
}
