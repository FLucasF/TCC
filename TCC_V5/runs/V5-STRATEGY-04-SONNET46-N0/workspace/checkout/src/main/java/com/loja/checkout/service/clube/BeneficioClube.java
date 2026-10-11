package com.loja.checkout.service.clube;

public interface BeneficioClube {
    String getNivel();
    boolean isFreteGratis();
    double calcularCredito(double subtotalProdutos);
    boolean temBrinde(double subtotalProdutos);
}
