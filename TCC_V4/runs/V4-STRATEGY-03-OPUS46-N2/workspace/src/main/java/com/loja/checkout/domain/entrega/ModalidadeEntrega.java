package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String getCodigo();

    BigDecimal calcularFrete(BigDecimal pesoKg);

    int getPrazoDias();

    boolean isDisponivel(BigDecimal pesoKg);
}
