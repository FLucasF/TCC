package com.loja.checkout.estrategia;

import com.loja.checkout.util.Arredondamento;

public class PagamentoCartao implements EstrategiaFormaPagamento {
    private double totalPedido = 0.0;

    @Override
    public double calcularAjuste(double total, int parcelas) {
        this.totalPedido = total;

        if (parcelas <= 3) {
            return 0.0;
        }

        double parcela = calcularParcelaComJuros(total, parcelas);
        double totalComJuros = Arredondamento.arredondar(parcela * parcelas);
        double ajuste = totalComJuros - total;

        return Arredondamento.arredondar(ajuste);
    }

    @Override
    public int getParcelasNoResponse(int parcelasRequisitadas) {
        return parcelasRequisitadas;
    }

    @Override
    public double getValorParcela(double total, int parcelas) {
        if (parcelas <= 3) {
            return Arredondamento.arredondar(total / parcelas);
        }

        return calcularParcelaComJuros(totalPedido, parcelas);
    }

    private double calcularParcelaComJuros(double totalBase, int parcelas) {
        double taxaMensal = 0.0199;
        double numerador = totalBase * taxaMensal;
        double denominador = 1.0 - Math.pow(1.0 + taxaMensal, -parcelas);
        return Arredondamento.arredondar(numerador / denominador);
    }
}
