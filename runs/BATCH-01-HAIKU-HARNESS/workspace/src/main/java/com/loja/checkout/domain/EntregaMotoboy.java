package com.loja.checkout.domain;

public class EntregaMotoboy implements ModalidadeEntrega {
    @Override
    public double calcularValor(double pesoKg) {
        return 18.00;
    }

    @Override
    public int obterPrazo() {
        return 0;
    }

    @Override
    public boolean estaDisponivel(double pesoTotalKg) {
        return pesoTotalKg <= 5.0;
    }

    @Override
    public String getNome() {
        return "MOTOBOY";
    }
}
