package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(double pesoKg);
    int getPrazo();
    void validarDisponibilidade(double pesoKg);
}
