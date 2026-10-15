package com.loja.checkout.resumo.pagamento;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** À vista, com 5% de desconto no total do pedido. */
public class Pix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public Parcelamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
        return new Parcelamento(totalFinal, totalFinal);
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }
}
