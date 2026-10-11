package com.loja.checkout.cupom;

import com.loja.checkout.dinheiro.Dinheiro;
import com.loja.checkout.pedido.ItemPedido;
import com.loja.checkout.pedido.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2Cupom implements Cupom {

    private static final int TAMANHO_LOTE = 3;

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
        BigDecimal total = pedido.itens().stream()
                .map(this::descontoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(total);
    }

    private BigDecimal descontoDoItem(ItemPedido item) {
        int unidadesGratis = item.quantidade() / TAMANHO_LOTE;
        return item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
    }
}
