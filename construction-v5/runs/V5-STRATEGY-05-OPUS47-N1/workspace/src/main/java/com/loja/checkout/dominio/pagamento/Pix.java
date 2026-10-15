package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Ajuste calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredonda(totalPedido.multiply(new BigDecimal("0.05")));
        BigDecimal totalFinal = Dinheiro.arredonda(totalPedido.subtract(desconto));
        BigDecimal ajuste = Dinheiro.arredonda(totalFinal.subtract(totalPedido));
        return new Ajuste(totalFinal, totalFinal, ajuste);
    }
}
