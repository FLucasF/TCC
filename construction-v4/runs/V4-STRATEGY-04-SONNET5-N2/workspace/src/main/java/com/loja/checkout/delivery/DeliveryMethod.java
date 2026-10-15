package com.loja.checkout.delivery;

import java.math.BigDecimal;

public interface DeliveryMethod {

    String getCodigo();

    int getPrazoDias();

    boolean isDisponivel(BigDecimal pesoTotalKg);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
}
