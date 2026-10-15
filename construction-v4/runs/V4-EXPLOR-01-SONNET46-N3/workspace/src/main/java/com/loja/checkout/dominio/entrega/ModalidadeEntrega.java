package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoKg);
    int prazoEntregaDias();
    void validarDisponibilidade(BigDecimal pesoKg);
}
