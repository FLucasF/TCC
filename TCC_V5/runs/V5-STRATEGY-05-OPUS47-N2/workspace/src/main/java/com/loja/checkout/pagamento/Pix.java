package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.AjustePagamento;
import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public final class Pix implements FormaPagamento {
    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
    @Override public boolean aceita(BigDecimal totalPedido) { return true; }

    @Override public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(DESCONTO));
        BigDecimal ajuste = desconto.negate();
        BigDecimal totalFinal = totalPedido.add(ajuste);
        return new AjustePagamento(ajuste, totalFinal);
    }
}
