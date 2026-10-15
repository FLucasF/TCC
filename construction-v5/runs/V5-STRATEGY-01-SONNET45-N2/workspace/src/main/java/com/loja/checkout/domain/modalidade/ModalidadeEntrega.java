package com.loja.checkout.domain.modalidade;

public interface ModalidadeEntrega {
    boolean aceita(double pesoTotal);
    ResultadoEntrega calcular(double pesoTotal);
}
