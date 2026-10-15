package com.loja.checkout.domain.entrega;

import com.loja.checkout.model.dto.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public class Motoboy implements ModalidadeEntrega {

    @Override
    public boolean estaDisponivel(List<ItemPedido> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pesoTotal.compareTo(new BigDecimal("5.00")) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(List<ItemPedido> itens) {
        return new BigDecimal("18.00");
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }
}
