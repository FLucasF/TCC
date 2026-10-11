package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivel(BigDecimal pesoTotalKg);

    BigDecimal calcularValor(BigDecimal pesoTotalKg);

    int prazoDias();
}
