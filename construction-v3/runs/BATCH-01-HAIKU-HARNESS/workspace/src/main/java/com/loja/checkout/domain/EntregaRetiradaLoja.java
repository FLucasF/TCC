package com.loja.checkout.domain;

public class EntregaRetiradaLoja implements ModalidadeEntrega {
    @Override
    public double calcularValor(double pesoKg) {
        return 0.0;
    }

    @Override
    public int obterPrazo() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(double pesoTotalKg) {
        return true;
    }

    @Override
    public String getNome() {
        return "RETIRADA_LOJA";
    }
}
