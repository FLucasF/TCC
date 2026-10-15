package com.loja.checkout.payment;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean aceitaParcelas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
