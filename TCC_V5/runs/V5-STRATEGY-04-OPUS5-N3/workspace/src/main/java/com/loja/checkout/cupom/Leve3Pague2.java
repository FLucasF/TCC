package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item, uma sai de graça. */
class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_BRINDE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.centavos(pedido.itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal unidadesGratis(ItemPedido item) {
        int gratis = item.quantidade() / UNIDADES_POR_BRINDE;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
