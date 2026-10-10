package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Pix: sempre a vista, com 5% de desconto no total do pedido. */
public final class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Liquidacao liquidar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(
                totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO)));
        return new Liquidacao(totalFinal, totalFinal);
    }
}
