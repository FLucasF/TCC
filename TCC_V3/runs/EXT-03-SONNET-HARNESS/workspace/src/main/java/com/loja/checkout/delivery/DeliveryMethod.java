package com.loja.checkout.delivery;

import java.math.BigDecimal;

/** Uma forma de entrega: cada uma sabe seu próprio custo, prazo e se atende o pedido. */
public interface DeliveryMethod {

    String getCodigo();

    boolean isDisponivel(BigDecimal pesoTotalKg);

    BigDecimal calcularCusto(BigDecimal pesoTotalKg);

    int getPrazoDias();
}
