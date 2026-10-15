package com.loja.checkout.estrategia;

import com.loja.checkout.util.Arredondamento;

public class PagamentoBoleto implements EstrategiaFormaPagamento {
    @Override
    public double calcularAjuste(double total, int parcelas) {
        return Arredondamento.arredondar(3.49);
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
