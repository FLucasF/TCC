package com.loja.checkout.domain.modalidade;

public class Motoboy implements ModalidadeEntrega {
    @Override
    public boolean aceita(double pesoTotal) {
        return pesoTotal <= 5.0;
    }

    @Override
    public ResultadoEntrega calcular(double pesoTotal) {
        return new ResultadoEntrega(18.00, 0);
    }
}
