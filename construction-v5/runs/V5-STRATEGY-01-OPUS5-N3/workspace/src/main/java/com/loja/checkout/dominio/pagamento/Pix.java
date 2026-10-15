package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Sempre à vista, com 5% de desconto no total do pedido. */
public final class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO));
        return new Cobranca(totalFinal, totalFinal);
    }
}
