package com.loja.checkout.service.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String getCodigo();

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int getPrazoDias();

    boolean isDisponivel(BigDecimal pesoTotalKg);
}
