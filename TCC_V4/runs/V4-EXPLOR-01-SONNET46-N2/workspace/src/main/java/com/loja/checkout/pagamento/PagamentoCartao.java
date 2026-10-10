package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class PagamentoCartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int MAX_SEM_JUROS = 3;

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean isDisponivel(BigDecimal total) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal valorParcela = total
                    .divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128)
                    .setScale(2, RoundingMode.HALF_EVEN);
            // sem juros: totalFinal é o próprio total do pedido
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), total, valorParcela);
        }

        // fórmula Price: PMT = PV * i / (1 - (1+i)^-n)
        BigDecimal onePlusRate = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal fator = onePlusRate.pow(-parcelas, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(fator);
        BigDecimal valorParcela = total.multiply(TAXA_JUROS)
                .divide(denominador, MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(total);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
