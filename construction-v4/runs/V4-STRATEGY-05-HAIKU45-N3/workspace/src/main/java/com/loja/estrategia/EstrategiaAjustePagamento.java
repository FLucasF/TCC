package com.loja.estrategia;

import java.math.BigDecimal;

public interface EstrategiaAjustePagamento {
    AjustePagamentoResult calcular(BigDecimal total, int parcelas);
    boolean validarParcelas(int parcelas);
}
