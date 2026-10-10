package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import java.math.BigDecimal;
import java.util.List;

/** Montagem de pedidos para os testes, com os itens dos exemplos do enunciado. */
final class PedidoDeTeste {

    static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");
    static final ItemRequest FONE = item("Fone", "199.90", 2, "0.25");
    static final ItemRequest MEIA = item("Meia", "19.90", 7, "0.10");

    private PedidoDeTeste() {
    }

    static ItemRequest item(String nome, String preco, Integer quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    static ResumoRequest pedido(List<ItemRequest> itens, String modalidade, String cupom,
            String formaPagamento, Integer parcelas, String nivelClube, String regiao) {
        return new ResumoRequest(itens, modalidade, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }
}
