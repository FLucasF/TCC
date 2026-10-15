package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface Entrega {
    BigDecimal calcularCusto(Double pesoKg);
    Integer prazo();
    boolean podeAtender(Double pesoKg);
    String nome();
}
