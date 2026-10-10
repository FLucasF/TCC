package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** A vista, com 5% de desconto sobre o total do pedido. */
public final class Pix implements PagamentoAVista {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("5");

    @Override
    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.percentual(PERCENTUAL_DESCONTO, totalPedido);
        BigDecimal totalFinal = Dinheiro.emCentavos(totalPedido.subtract(desconto));
        return new Pagamento(totalFinal, totalFinal);
    }
}
