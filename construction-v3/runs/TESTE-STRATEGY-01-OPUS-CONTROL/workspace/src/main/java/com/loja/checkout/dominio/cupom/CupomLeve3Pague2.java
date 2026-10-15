package com.loja.checkout.dominio.cupom;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal desconto = contexto.pedido().itens().stream()
                .map(this::descontoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }

    private BigDecimal descontoDoItem(ItemPedido item) {
        int gratuitas = item.quantidade() / 3;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratuitas));
    }
}
