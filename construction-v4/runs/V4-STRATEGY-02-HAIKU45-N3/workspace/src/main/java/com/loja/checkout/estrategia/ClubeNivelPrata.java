package com.loja.checkout.estrategia;

import com.loja.checkout.util.Arredondamento;

public class ClubeNivelPrata implements EstrategiaClube {
    @Override
    public double calcularCredito(double subtotalProdutos) {
        return Arredondamento.arredondar(subtotalProdutos * 0.02);
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
