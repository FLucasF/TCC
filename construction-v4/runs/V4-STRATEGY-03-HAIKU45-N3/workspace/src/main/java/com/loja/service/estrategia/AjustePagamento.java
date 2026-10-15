package com.loja.service.estrategia;

import java.math.BigDecimal;

public interface AjustePagamento {
    BigDecimal calcularAjuste(BigDecimal total, int parcelas);
    int obterParcelas(Integer parcelasRequest);
}
