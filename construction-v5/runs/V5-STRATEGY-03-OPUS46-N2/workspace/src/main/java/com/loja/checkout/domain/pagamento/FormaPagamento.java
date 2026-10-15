package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    boolean parcelamentoValido(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
