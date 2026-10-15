package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String getCodigo();

    BigDecimal calcularFrete(BigDecimal pesoTotal);

    int getPrazoDias();

    boolean isDisponivel(BigDecimal pesoTotal);
}
