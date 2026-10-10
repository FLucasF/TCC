package com.loja.estrategia;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoCartao implements EstrategiaAjustePagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public AjustePagamentoResult calcular(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = total.divide(
                    new BigDecimal(parcelas),
                    2,
                    RoundingMode.HALF_EVEN
            );
            BigDecimal totalCalculado = valorParcela.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalCalculado.subtract(total);
            return new AjustePagamentoResult(ajuste, parcelas, valorParcela);
        } else {
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
            BigDecimal potencia = umMaisTaxa.pow(parcelas);
            BigDecimal expoente = BigDecimal.ONE.divide(potencia, 15, RoundingMode.HALF_EVEN);
            BigDecimal denominador = BigDecimal.ONE.subtract(expoente);
            BigDecimal valorParcela = total.multiply(TAXA_JUROS).divide(
                    denominador,
                    2,
                    RoundingMode.HALF_EVEN
            );
            BigDecimal totalCalculado = valorParcela.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalCalculado.subtract(total);
            return new AjustePagamentoResult(ajuste, parcelas, valorParcela);
        }
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }
}
