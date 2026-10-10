package com.loja.checkout.estrategia;

import com.loja.checkout.util.Arredondamento;

public class PagamentoPix implements EstrategiaFormaPagamento {
    @Override
    public double calcularAjuste(double total, int parcelas) {
        double desconto = Arredondamento.arredondar(total * 0.05);
        return -desconto;
    }

    @Override
    public int getParcelasNoResponse(int parcelasRequisitadas) {
        return 1;
    }

    @Override
    public double getValorParcela(double total, int parcelas) {
        return total;
    }
}
