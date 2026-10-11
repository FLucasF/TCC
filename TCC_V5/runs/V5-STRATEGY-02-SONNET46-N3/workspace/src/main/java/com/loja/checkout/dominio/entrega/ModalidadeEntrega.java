package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoKgTotal);
    int prazoEntregaDias();
    boolean estaDisponivel(BigDecimal pesoKgTotal);
}
