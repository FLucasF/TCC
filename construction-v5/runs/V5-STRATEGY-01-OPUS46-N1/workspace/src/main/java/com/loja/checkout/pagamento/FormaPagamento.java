package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean parcelamentoValido(int parcelas);

    boolean disponivel(BigDecimal totalPedido, int parcelas);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
