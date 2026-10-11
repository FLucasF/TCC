package com.loja.checkout.service.pagamento;

import com.loja.checkout.service.Arredondamento;
import org.springframework.stereotype.Component;

@Component
public class PagamentoPix implements FormaPagamento {
    @Override
    public String getCodigo() { return "PIX"; }

    @Override
    public boolean parcelasValidas(int parcelas) { return parcelas == 1; }

    @Override
    public boolean aceitaTotal(double total) { return true; }

    @Override
    public ResultadoPagamento calcular(double totalPedido, int parcelas) {
        double desconto = Arredondamento.arredondar(totalPedido * 0.05);
        double totalFinal = Arredondamento.arredondar(totalPedido - desconto);
        double ajuste = Arredondamento.arredondar(totalFinal - totalPedido);
        return new ResultadoPagamento(totalFinal, ajuste, 1, totalFinal);
    }
}
