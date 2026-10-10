package com.loja.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean aceitaParcelas(int parcelas);
    boolean aceita(BigDecimal totalPedido);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
