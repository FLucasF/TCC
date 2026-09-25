package com.loja.checkout.domain;

public class EntregaEconomica implements ModalidadeEntrega {
    @Override
    public double calcularValor(double pesoKg) {
        return 12.00 + (2.00 * pesoKg);
    }

    @Override
    public int obterPrazo() {
        return 7;
    }

    @Override
    public boolean estaDisponivel(double pesoTotalKg) {
        return true;
    }

    @Override
    public String getNome() {
        return "ECONOMICA";
    }
}
