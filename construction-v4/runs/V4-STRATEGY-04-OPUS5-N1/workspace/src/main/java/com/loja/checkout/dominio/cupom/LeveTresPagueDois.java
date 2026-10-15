package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Centavos;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
public final class LeveTresPagueDois implements Cupom {

    private static final int UNIDADES_PARA_GANHAR_UMA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Centavos.arredondar(pedido.itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal unidadesGratis(ItemPedido item) {
        int gratis = item.quantidade() / UNIDADES_PARA_GANHAR_UMA;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
