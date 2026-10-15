package com.loja.model.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas);
    boolean aceitaParcelas(int parcelas);
    boolean aceitaPedido(BigDecimal totalPedido);
}
