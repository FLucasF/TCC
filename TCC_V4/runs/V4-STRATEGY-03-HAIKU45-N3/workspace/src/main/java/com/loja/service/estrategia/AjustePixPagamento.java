package com.loja.service.estrategia;

import java.math.BigDecimal;

public class AjustePixPagamento implements AjustePagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
        return desconto.negate();
    }

    @Override
    public int obterParcelas(Integer parcelasRequest) {
        return 1;
    }
}
