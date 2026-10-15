package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.ModalidadeEntrega;

import java.math.BigDecimal;

/** Ganha 5% em credito, nao paga frete nunca e, acima de R$ 500,00, leva brinde. */
public final class Ouro implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal PRODUTOS_PARA_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CREDITO);
    }

    /** Nao paga frete nunca: sai zerado no resumo, qualquer que seja a entrega. */
    @Override
    public BigDecimal frete(ModalidadeEntrega entrega, Pedido pedido) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(PRODUTOS_PARA_BRINDE) > 0;
    }
}
