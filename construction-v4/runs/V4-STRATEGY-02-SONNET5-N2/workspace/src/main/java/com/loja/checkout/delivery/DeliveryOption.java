package com.loja.checkout.delivery;

import java.math.BigDecimal;

public interface DeliveryOption {

    String codigo();

    boolean disponivel(BigDecimal pesoKg);

    BigDecimal calcularFrete(BigDecimal pesoKg);

    int prazoDias();
}
