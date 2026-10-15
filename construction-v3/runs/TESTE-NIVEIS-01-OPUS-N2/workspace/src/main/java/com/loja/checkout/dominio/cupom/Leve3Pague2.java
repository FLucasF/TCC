package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_GRATUITA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.itens().stream()
                .map(this::descontoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal descontoDoItem(Item item) {
        int gratuitas = item.quantidade() / UNIDADES_POR_GRATUITA;
        return Dinheiro.centavos(item.precoUnitario().multiply(BigDecimal.valueOf(gratuitas)));
    }
}
