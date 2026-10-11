package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivel(double pesoTotalKg);

    BigDecimal calcularFrete(double pesoTotalKg);

    int prazoEntregaDias();
}
