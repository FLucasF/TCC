package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface CalculoEntrega {

    String getCodigo();

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int getPrazoDias();

    boolean isDisponivel(BigDecimal pesoTotalKg);
}
