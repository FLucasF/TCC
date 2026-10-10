package com.loja.checkout.domain.strategy;

public interface AjustePagamentoCalculador {
    double calcularAjuste(double total, int parcelas);
    int[] parcelasPermitidas();
    boolean ehDisponivel(double total);
}
