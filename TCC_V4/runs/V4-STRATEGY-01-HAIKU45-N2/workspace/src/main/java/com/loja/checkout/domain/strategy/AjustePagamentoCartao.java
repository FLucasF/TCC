package com.loja.checkout.domain.strategy;

import com.loja.checkout.util.Arredondador;

public class AjustePagamentoCartao implements AjustePagamentoCalculador {
    private static final double TAXA_JUROS = 0.0199;

    @Override
    public double calcularAjuste(double total, int parcelas) {
        if (parcelas <= 3) {
            return 0;
        }

        double valorParcela = calcularParcelaComJuros(total, parcelas);
        double valorFinal = Arredondador.arredondarParaCentavos(valorParcela * parcelas);
        double ajuste = valorFinal - total;
        return Arredondador.arredondarParaCentavos(ajuste);
    }

    @Override
    public int[] parcelasPermitidas() {
        return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    }

    @Override
    public boolean ehDisponivel(double total) {
        return true;
    }

    private double calcularParcelaComJuros(double total, int parcelas) {
        double denominador = 1 - Math.pow(1 + TAXA_JUROS, -parcelas);
        double valorParcela = total * TAXA_JUROS / denominador;
        return Arredondador.arredondarParaCentavos(valorParcela);
    }
}
