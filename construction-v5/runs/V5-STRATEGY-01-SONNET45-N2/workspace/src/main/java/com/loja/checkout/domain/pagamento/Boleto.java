package com.loja.checkout.domain.pagamento;

public class Boleto implements FormaPagamento {
    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(double totalPedido) {
        return totalPedido <= 1000.00;
    }

    @Override
    public ResultadoPagamento calcular(double totalPedido, int parcelas) {
        double totalFinal = arredondar(totalPedido + 3.49);
        double ajuste = arredondar(totalFinal - totalPedido);

        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
