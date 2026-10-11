package com.loja.checkout.pagamento;

import com.loja.checkout.calculo.Dinheiro;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.cents(totalPedido.multiply(DESCONTO));
        BigDecimal totalFinal = Dinheiro.cents(totalPedido.subtract(desconto));
        BigDecimal ajuste = Dinheiro.cents(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(totalFinal, 1, totalFinal, ajuste);
    }
}
