package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    boolean parcelasPermitidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);
}
