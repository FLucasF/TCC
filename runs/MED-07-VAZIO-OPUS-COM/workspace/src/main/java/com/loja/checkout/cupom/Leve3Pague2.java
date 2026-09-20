package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Entrega;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_GRUPO = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, Entrega entrega) {
        return Dinheiro.arredondar(pedido.itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal unidadesGratis(ItemPedido item) {
        int gratis = item.quantidade() / UNIDADES_POR_GRUPO;
        return Dinheiro.arredondar(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
    }
}
