package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public class Expressa implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal frete = new BigDecimal("25.00")
            .add(new BigDecimal("4.50").multiply(pesoTotal));

        return Dinheiro.arredondar(frete);
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        return true;
    }
}
