package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal LIMITE_PESO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pesoTotal.compareTo(LIMITE_PESO) <= 0;
    }
}
