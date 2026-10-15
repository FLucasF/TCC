package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    String getCodigo();
    BigDecimal calcularFrete(double pesoKg);
    int getPrazoEntregaDias();
    void verificarDisponibilidade(double pesoKg);
}
