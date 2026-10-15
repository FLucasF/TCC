package com.loja.checkout.domain;

public interface ModalidadeEntrega {
    double calcularValor(double pesoKg);
    int obterPrazo();
    boolean estaDisponivel(double pesoTotalKg);
    String getNome();
}
