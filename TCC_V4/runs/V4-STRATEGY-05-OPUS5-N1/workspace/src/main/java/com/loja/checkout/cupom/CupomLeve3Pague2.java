package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_GRATUITA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.emCentavos(pedido.itens().stream()
                .map(this::descontoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal descontoDoItem(ItemPedido item) {
        int gratuitas = item.quantidade() / UNIDADES_POR_GRATUITA;
        return Dinheiro.emCentavos(item.precoUnitario().multiply(BigDecimal.valueOf(gratuitas)));
    }
}
