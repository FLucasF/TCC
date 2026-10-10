package com.loja.estrategia;

import java.math.BigDecimal;

public class PagamentoPix implements EstrategiaAjustePagamento {
    @Override
    public AjustePagamentoResult calcular(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
        BigDecimal totalComDesconto = total.subtract(desconto);
        return new AjustePagamentoResult(desconto.negate(), 1, totalComDesconto);
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas == 1;
    }
}
