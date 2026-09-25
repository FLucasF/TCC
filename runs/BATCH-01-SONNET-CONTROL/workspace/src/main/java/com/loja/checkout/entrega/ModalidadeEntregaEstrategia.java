package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntregaEstrategia {

    String getCodigo();

    boolean disponivel(BigDecimal pesoTotalKg);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoEntregaDias();
}
