package com.loja.checkout.estrategia;

public class ClubeNivelBronze implements EstrategiaClube {
    @Override
    public double calcularCredito(double subtotalProdutos) {
        return 0.0;
    }

    @Override
    public boolean ehIsento(String modalidadeEntrega) {
        return false;
    }

    @Override
    public boolean temBrinde(double subtotalProdutos) {
        return false;
    }
}
