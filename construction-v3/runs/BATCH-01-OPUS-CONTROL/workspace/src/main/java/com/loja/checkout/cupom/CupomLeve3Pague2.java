package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    private static final int A_CADA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    /** Precisa de pelo menos um item com 3 unidades ou mais para valer alguma coisa. */
    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.pedido().itens().stream()
                .anyMatch(item -> item.unidadesGratis(A_CADA) > 0);
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        BigDecimal desconto = contexto.pedido().itens().stream()
                .map(this::descontoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }

    private BigDecimal descontoDoItem(ItemPedido item) {
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.unidadesGratis(A_CADA)));
    }
}
