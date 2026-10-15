package com.loja.checkout.domain.pagamento.impl;

import com.loja.checkout.domain.pagamento.FormaPagamentoStrategy;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class PagamentoCartao implements FormaPagamentoStrategy {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_EVEN);

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivelParaTotal(BigDecimal total) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(total, valorParcela, parcelas);
        }

        // Tabela Price: parcela = P × i / (1 − (1 + i)^−n)
        BigDecimal potencia = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, MC);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, MC), MC);
        BigDecimal valorParcela = total.multiply(TAXA_MENSAL, MC)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
    }
}
