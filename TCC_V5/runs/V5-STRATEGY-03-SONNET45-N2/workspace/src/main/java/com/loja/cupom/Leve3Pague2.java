package com.loja.cupom;

import com.loja.domain.Item;
import com.loja.domain.Pedido;
import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class Leve3Pague2 implements Cupom {

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete) {
        BigDecimal desconto = pedido.itens().stream()
            .map(this::calcularDescontoItem)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Arredondador.arredondar(desconto);
    }

    private BigDecimal calcularDescontoItem(Item item) {
        int unidadesGratis = item.quantidade() / 3;
        return item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
    }
}
