package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean parcelasValidas(int parcelas);
    boolean disponivel(BigDecimal totalPedido);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
