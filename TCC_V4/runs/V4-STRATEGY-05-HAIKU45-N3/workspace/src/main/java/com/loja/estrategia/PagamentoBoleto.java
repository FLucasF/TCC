package com.loja.estrategia;

import java.math.BigDecimal;

public class PagamentoBoleto implements EstrategiaAjustePagamento {
    @Override
    public AjustePagamentoResult calcular(BigDecimal total, int parcelas) {
        BigDecimal taxa = new BigDecimal("3.49");
        BigDecimal totalComTaxa = total.add(taxa);
        return new AjustePagamentoResult(taxa, 1, totalComTaxa);
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas == 1;
    }
}
