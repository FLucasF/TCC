package com.loja.checkout.domain;

public class FormaPagamentoCartao implements FormaPagamento {
    @Override
    public double calcularAjuste(double total, int parcelas) {
        if (parcelas <= 3) {
            return 0.0;
        }
        double taxaMensal = 0.0199;
        double parcela = total * taxaMensal / (1 - Math.pow(1 + taxaMensal, -parcelas));
        parcela = Arredondamento.arredondarParaCentavos(parcela);
        double totalComJuros = parcela * parcelas;
        return totalComJuros - total;
    }

    @Override
    public int obterParcelasMaximas() {
        return 12;
    }

    @Override
    public boolean estaDisponivel(double total, int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public String getNome() {
        return "CARTAO";
    }
}
