package com.loja.checkout.domain.pagamento;

public class Pix implements FormaPagamento {
    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(double totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(double totalPedido, int parcelas) {
        double desconto = arredondar(totalPedido * 0.05);
        double totalFinal = arredondar(totalPedido - desconto);
        double ajuste = arredondar(totalFinal - totalPedido);

        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
