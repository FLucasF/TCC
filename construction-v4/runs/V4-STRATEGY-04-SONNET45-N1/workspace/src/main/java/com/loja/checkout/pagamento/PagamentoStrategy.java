package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface PagamentoStrategy {
    boolean validarParcelas(int parcelas);
    boolean estaDisponivel(BigDecimal totalPedido);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
}
