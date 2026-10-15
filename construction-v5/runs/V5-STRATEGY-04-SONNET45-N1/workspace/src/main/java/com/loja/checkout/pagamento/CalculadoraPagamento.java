package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface CalculadoraPagamento {
    boolean podeAtender(BigDecimal totalPedido);
    boolean aceitaParcelas(int parcelas);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
}
