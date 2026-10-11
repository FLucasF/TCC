package com.loja.checkout.domain;

public enum Regiao {
    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    private final double percentualSeguro;

    Regiao(double percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public double calcularSeguro(double valorProdutos) {
        return arredondar(valorProdutos * percentualSeguro);
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
