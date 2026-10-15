package com.loja.checkout.domain;

public interface FormaPagamento {
    double calcularAjuste(double total, int parcelas);
    int obterParcelasMaximas();
    boolean estaDisponivel(double total, int parcelas);
    String getNome();
}
