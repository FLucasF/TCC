package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean aceitaParcelas(int numParcelas);
    boolean estaDisponivel(BigDecimal totalPedido);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
