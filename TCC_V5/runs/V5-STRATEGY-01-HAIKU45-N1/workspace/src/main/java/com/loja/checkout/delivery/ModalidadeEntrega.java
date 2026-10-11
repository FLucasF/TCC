package com.loja.checkout.delivery;

public interface ModalidadeEntrega {
    double calcularFrete(double pesoTotalKg);
    int getPrazoEntregaDias();
    boolean estaDisponivel(double pesoTotalKg);
    String getNome();
}
