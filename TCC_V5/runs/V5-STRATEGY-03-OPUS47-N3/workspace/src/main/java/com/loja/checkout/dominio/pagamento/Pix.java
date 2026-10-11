package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Pix implements Pagamento {
    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override public String codigo() { return "PIX"; }
    @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
    @Override public boolean disponivelPara(BigDecimal totalPedido) { return true; }
    @Override public Ajuste calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(DESCONTO));
        BigDecimal ajuste = desconto.negate();
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(ajuste));
        return new Ajuste(ajuste, totalFinal, totalFinal);
    }
}
