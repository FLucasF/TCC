package com.loja.checkout.domain.strategy;

import com.loja.checkout.util.Arredondador;

public class AjustePagamentoBoleto implements AjustePagamentoCalculador {
    private static final double TARIFA = 3.49;

    @Override
    public double calcularAjuste(double total, int parcelas) {
        return Arredondador.arredondarParaCentavos(TARIFA);
    }

    @Override
    public int[] parcelasPermitidas() {
        return new int[]{1};
    }

    @Override
    public boolean ehDisponivel(double total) {
        return total <= 1000.0;
    }
}
