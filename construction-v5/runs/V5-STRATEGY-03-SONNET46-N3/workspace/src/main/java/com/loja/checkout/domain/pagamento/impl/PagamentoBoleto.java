package com.loja.checkout.domain.pagamento.impl;

import com.loja.checkout.domain.pagamento.FormaPagamentoStrategy;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoBoleto implements FormaPagamentoStrategy {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivelParaTotal(BigDecimal total) {
        return total.compareTo(LIMITE) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        BigDecimal totalFinal = total.add(TARIFA).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(totalFinal, totalFinal, 1);
    }
}
