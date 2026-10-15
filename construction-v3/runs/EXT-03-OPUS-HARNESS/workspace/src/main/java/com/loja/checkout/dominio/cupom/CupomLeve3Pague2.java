package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.pedido().itens().stream()
                .map(this::gratuidadeDo)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal gratuidadeDo(Item item) {
        long gratuitas = item.quantidade() / 3;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratuitas));
    }
}
