package com.loja.checkout.domain.modalidade;

public class Expressa implements ModalidadeEntrega {
    @Override
    public boolean aceita(double pesoTotal) {
        return true;
    }

    @Override
    public ResultadoEntrega calcular(double pesoTotal) {
        double valor = arredondar(25.00 + (4.50 * pesoTotal));
        return new ResultadoEntrega(valor, 2);
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
