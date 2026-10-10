package com.loja.checkout.estrategia;

import com.loja.checkout.util.Arredondamento;

public class ClubeNivelOuro implements EstrategiaClube {
    @Override
    public double calcularCredito(double subtotalProdutos) {
        return Arredondamento.arredondar(subtotalProdutos * 0.05);
    }

    @Override
    public boolean ehIsento(String modalidadeEntrega) {
        return true;
    }

    @Override
    public boolean temBrinde(double subtotalProdutos) {
        return subtotalProdutos > 500.00;
    }
}
