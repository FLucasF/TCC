package com.loja.checkout.domain.strategy;

import com.loja.checkout.util.Arredondador;

public class AjustePagamentoPix implements AjustePagamentoCalculador {
    @Override
    public double calcularAjuste(double total, int parcelas) {
        double desconto = total * 0.05;
        desconto = Arredondador.arredondarParaCentavos(desconto);
        return -desconto;
    }

    @Override
    public int[] parcelasPermitidas() {
        return new int[]{1};
    }

    @Override
    public boolean ehDisponivel(double total) {
        return true;
    }
}
