package com.loja.checkout.service.entrega;

public interface ModalidadeEntrega {
    String getCodigo();
    boolean aceita(double pesoTotalKg);
    ResultadoFrete calcular(double pesoTotalKg);
}
