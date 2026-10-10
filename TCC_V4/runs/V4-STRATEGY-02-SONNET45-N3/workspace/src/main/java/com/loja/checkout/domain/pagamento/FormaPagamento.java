package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    boolean validarParcelas(int parcelas);
    boolean aceitaTotal(BigDecimal totalPedido);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal totalFinal, int parcelas);
}
