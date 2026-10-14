package com.loja.pedidos.cupom;

import java.math.BigDecimal;
import java.util.List;

import com.loja.pedidos.entrega.ModalidadeEntrega;

public record ContextoCupom(
        String cpf,
        List<LinhaItem> itens,
        BigDecimal subtotal,
        ModalidadeEntrega modalidade,
        BigDecimal frete) {
}
