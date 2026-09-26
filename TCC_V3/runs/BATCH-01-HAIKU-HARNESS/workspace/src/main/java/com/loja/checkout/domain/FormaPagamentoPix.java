package com.loja.checkout.domain;

public class FormaPagamentoPix implements FormaPagamento {
    @Override
    public double calcularAjuste(double total, int parcelas) {
        return -(total * 0.05);
    }

    @Override
    public int obterParcelasMaximas() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(double total, int parcelas) {
        return parcelas == 1;
    }

    @Override
    public String getNome() {
        return "PIX";
    }
}
