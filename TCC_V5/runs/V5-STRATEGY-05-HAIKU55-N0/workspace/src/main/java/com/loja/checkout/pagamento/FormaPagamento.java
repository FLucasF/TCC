package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean aceitaParcelas(int parcelas);

    boolean atende(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
