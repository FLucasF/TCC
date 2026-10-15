package com.loja.checkout.payment;

import java.math.BigDecimal;

public interface PaymentMethod {

    String codigo();

    boolean parcelasPermitidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
