package com.loja.checkout.domain.entrega;

import com.loja.checkout.model.dto.ItemPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Economica implements ModalidadeEntrega {

    @Override
    public boolean estaDisponivel(List<ItemPedido> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(List<ItemPedido> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BigDecimal("12.00")
            .add(pesoTotal.multiply(new BigDecimal("2.00")))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }
}
