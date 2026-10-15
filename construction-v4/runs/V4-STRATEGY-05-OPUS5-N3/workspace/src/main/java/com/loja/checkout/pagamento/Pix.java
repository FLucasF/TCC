package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** A vista, com 5% de desconto no total do pedido. */
public final class Pix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

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
        BigDecimal desconto = Dinheiro.percentual(totalPedido, PERCENTUAL_DESCONTO);
        BigDecimal totalFinal = Dinheiro.arredonda(totalPedido.subtract(desconto));
        return new Cobranca(totalFinal, totalFinal);
    }
}
