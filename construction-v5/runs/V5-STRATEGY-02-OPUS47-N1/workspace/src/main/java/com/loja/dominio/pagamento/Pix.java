package com.loja.dominio.pagamento;

import com.loja.dominio.Dinheiro;

import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
    @Override public boolean atende(BigDecimal totalPedido) { return true; }

    @Override public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorDesconto = Dinheiro.centavos(totalPedido.multiply(DESCONTO));
        BigDecimal ajuste = valorDesconto.negate();
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(ajuste));
        return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
    }
}
