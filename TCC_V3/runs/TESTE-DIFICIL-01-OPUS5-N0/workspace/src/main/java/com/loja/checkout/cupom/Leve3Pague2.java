package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** LEVE3PAGUE2: a cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.centavos(pedido.itens().stream()
                .map(this::gratisDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal gratisDoItem(Item item) {
        int unidadesGratis = item.quantidade() / 3;
        return item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
    }
}
