package com.loja.checkout.estrategia;

public interface EstrategiaFormaPagamento {
    double calcularAjuste(double total, int parcelas);
    int getParcelasNoResponse(int parcelasRequisitadas);
    double getValorParcela(double total, int parcelas);
}
