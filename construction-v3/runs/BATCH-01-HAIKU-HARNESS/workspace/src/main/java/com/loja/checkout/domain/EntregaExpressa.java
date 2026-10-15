package com.loja.checkout.domain;

public class EntregaExpressa implements ModalidadeEntrega {
    @Override
    public double calcularValor(double pesoKg) {
        return 25.00 + (4.50 * pesoKg);
    }

    @Override
    public int obterPrazo() {
        return 2;
    }

    @Override
    public boolean estaDisponivel(double pesoTotalKg) {
        return true;
    }

    @Override
    public String getNome() {
        return "EXPRESSA";
    }
}
