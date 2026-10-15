package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas);
    boolean verificarDisponibilidade(BigDecimal totalPedido, int parcelas);
}
