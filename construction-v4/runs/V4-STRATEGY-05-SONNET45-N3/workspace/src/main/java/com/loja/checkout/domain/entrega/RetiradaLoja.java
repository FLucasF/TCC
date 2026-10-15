package com.loja.checkout.domain.entrega;

import com.loja.checkout.model.dto.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public boolean estaDisponivel(List<ItemPedido> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(List<ItemPedido> itens) {
        return new BigDecimal("0.00");
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }
}
