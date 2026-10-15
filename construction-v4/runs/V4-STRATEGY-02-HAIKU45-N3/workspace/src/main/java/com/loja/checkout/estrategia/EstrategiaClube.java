package com.loja.checkout.estrategia;

public interface EstrategiaClube {
    double calcularCredito(double subtotalProdutos);
    boolean ehIsento(String modalidadeEntrega);
    boolean temBrinde(double subtotalProdutos);
}
