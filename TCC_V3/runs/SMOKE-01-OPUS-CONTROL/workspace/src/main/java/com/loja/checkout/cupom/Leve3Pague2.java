package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal desconto = contexto.pedido().itens().stream()
                .map(this::descontoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.valor(desconto);
    }

    private BigDecimal descontoDoItem(Item item) {
        int unidadesGratis = item.quantidade() / 3;
        return item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
    }
}
