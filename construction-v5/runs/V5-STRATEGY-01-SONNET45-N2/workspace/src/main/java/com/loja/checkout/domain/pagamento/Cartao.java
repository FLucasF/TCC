package com.loja.checkout.domain.pagamento;

public class Cartao implements FormaPagamento {
    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivel(double totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(double totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return calcularSemJuros(totalPedido, parcelas);
        } else {
            return calcularComJuros(totalPedido, parcelas);
        }
    }

    private ResultadoPagamento calcularSemJuros(double totalPedido, int parcelas) {
        double valorParcela = arredondar(totalPedido / parcelas);
        double totalFinal = arredondar(valorParcela * parcelas);
        double ajuste = arredondar(totalFinal - totalPedido);

        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }

    private ResultadoPagamento calcularComJuros(double totalPedido, int parcelas) {
        double taxa = 0.0199;
        double valorParcela = totalPedido * taxa / (1 - Math.pow(1 + taxa, -parcelas));
        valorParcela = arredondar(valorParcela);
        double totalFinal = arredondar(valorParcela * parcelas);
        double ajuste = arredondar(totalFinal - totalPedido);

        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }

    private static double arredondar(double valor) {
        return Math.rint(valor * 100) / 100;
    }
}
