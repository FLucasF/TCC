package com.loja.checkout.service.pagamento;

import com.loja.checkout.service.Arredondamento;
import org.springframework.stereotype.Component;

@Component
public class PagamentoCartao implements FormaPagamento {
    private static final double TAXA_MENSAL = 0.0199;

    @Override
    public String getCodigo() { return "CARTAO"; }

    @Override
    public boolean parcelasValidas(int parcelas) { return parcelas >= 1 && parcelas <= 12; }

    @Override
    public boolean aceitaTotal(double total) { return true; }

    @Override
    public ResultadoPagamento calcular(double totalPedido, int parcelas) {
        if (parcelas <= 3) {
            double valorParcela = Arredondamento.arredondar(totalPedido / parcelas);
            double totalFinal = Arredondamento.arredondar(valorParcela * parcelas);
            double ajuste = Arredondamento.arredondar(totalFinal - totalPedido);
            return new ResultadoPagamento(totalFinal, ajuste, parcelas, valorParcela);
        }
        double taxa = TAXA_MENSAL;
        double valorParcela = Arredondamento.arredondar(
                totalPedido * taxa / (1 - Math.pow(1 + taxa, -parcelas))
        );
        double totalFinal = Arredondamento.arredondar(valorParcela * parcelas);
        double ajuste = Arredondamento.arredondar(totalFinal - totalPedido);
        return new ResultadoPagamento(totalFinal, ajuste, parcelas, valorParcela);
    }
}
