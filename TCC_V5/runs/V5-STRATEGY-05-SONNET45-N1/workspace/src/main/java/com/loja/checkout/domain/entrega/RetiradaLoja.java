package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return new BigDecimal("0.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        return true;
    }
}
