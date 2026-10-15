package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    void validarParcelas(int parcelas);
    void validarDisponibilidade(BigDecimal totalPedido);
    BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularParcela(BigDecimal totalFinal, int parcelas);
}
