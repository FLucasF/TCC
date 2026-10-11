package com.loja.checkout.domain.modalidade;

public class Economica implements ModalidadeEntrega {
    @Override
    public boolean aceita(double pesoTotal) {
        return true;
    }

    @Override
    public ResultadoEntrega calcular(double pesoTotal) {
        double valor = arredondar(12.00 + (2.00 * pesoTotal));
        return new ResultadoEntrega(valor, 7);
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
