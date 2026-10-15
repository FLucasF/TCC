package com.loja.checkout.service.pagamento;

import com.loja.checkout.service.Arredondamento;
import org.springframework.stereotype.Component;

@Component
public class PagamentoBoleto implements FormaPagamento {
    private static final double TARIFA = 3.49;

    @Override
    public String getCodigo() { return "BOLETO"; }

    @Override
    public boolean parcelasValidas(int parcelas) { return parcelas == 1; }

    @Override
    public boolean aceitaTotal(double total) { return total <= 1000.00; }

    @Override
    public ResultadoPagamento calcular(double totalPedido, int parcelas) {
        double totalFinal = Arredondamento.arredondar(totalPedido + TARIFA);
        double ajuste = Arredondamento.arredondar(totalFinal - totalPedido);
        return new ResultadoPagamento(totalFinal, ajuste, 1, totalFinal);
    }
}
