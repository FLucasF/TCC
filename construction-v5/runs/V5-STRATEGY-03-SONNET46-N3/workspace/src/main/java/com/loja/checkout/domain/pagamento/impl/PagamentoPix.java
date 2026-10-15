package com.loja.checkout.domain.pagamento.impl;

import com.loja.checkout.domain.pagamento.FormaPagamentoStrategy;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoPix implements FormaPagamentoStrategy {

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivelParaTotal(BigDecimal total) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = total.subtract(desconto).setScale(2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(totalFinal, totalFinal, 1);
    }
}
