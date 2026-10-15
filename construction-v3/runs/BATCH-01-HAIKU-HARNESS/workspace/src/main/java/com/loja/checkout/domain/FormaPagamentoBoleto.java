package com.loja.checkout.domain;

public class FormaPagamentoBoleto implements FormaPagamento {
    @Override
    public double calcularAjuste(double total, int parcelas) {
        return 3.49;
    }

    @Override
    public int obterParcelasMaximas() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(double total, int parcelas) {
        return parcelas == 1 && total <= 1000.00;
    }

    @Override
    public String getNome() {
        return "BOLETO";
    }
}
