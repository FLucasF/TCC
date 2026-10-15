package com.loja.checkout.payment;

import java.math.BigDecimal;

public interface PaymentMethod {

    String getCodigo();

    boolean isParcelasValida(int parcelas);

    boolean isDisponivel(BigDecimal totalPedido);

    PaymentResult calcular(BigDecimal totalPedido, int parcelas);
}
