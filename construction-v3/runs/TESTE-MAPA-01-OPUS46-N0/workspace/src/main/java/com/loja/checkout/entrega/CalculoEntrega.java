package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface CalculoEntrega {

    String codigo();

    ResultadoEntrega calcular(BigDecimal pesoKg);

    default void validar(BigDecimal pesoKg) {
    }
}
