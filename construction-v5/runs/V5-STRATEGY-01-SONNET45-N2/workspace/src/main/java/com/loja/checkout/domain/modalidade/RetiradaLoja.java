package com.loja.checkout.domain.modalidade;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public boolean aceita(double pesoTotal) {
        return true;
    }

    @Override
    public ResultadoEntrega calcular(double pesoTotal) {
        return new ResultadoEntrega(0.0, 1);
    }
}
