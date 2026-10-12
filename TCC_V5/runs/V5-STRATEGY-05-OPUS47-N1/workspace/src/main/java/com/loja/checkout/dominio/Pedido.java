package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(
        List<Item> itens,
        ModalidadeEntrega modalidade,
        Cupom cupom,
        FormaPagamento forma,
        int parcelas,
        NivelClube nivel,
        Regiao regiao
) {
    public BigDecimal subtotalProdutos() {
        return itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
