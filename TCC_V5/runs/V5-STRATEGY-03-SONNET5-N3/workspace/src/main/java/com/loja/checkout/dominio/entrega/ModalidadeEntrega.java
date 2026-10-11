package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    BigDecimal calcularFrete(BigDecimal pesoKg);

    default void validarDisponibilidade(BigDecimal pesoKg) {
    }
}
