package com.loja.service.estrategia;

import java.math.BigDecimal;

public class AjusteBoletoPagamento implements AjustePagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        return TARIFA;
    }

    @Override
    public int obterParcelas(Integer parcelasRequest) {
        return 1;
    }
}
