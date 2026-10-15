package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoPedidoRequest;

import java.math.BigDecimal;
import java.util.List;

/** Itens e pedidos usados nos exemplos conferidos pelo financeiro. */
final class ResumoPedidoFixtures {

    static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");
    static final ItemRequest FONE = item("Fone", "199.90", 2, "0.25");
    static final ItemRequest MEIA = item("Meia", "19.90", 7, "0.10");

    private ResumoPedidoFixtures() {
    }

    static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    static ResumoPedidoRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
                                      String pagamento, Integer parcelas, String clube, String regiao) {
        return new ResumoPedidoRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }

    /** Camiseta × 2 + Tênis × 1: subtotal 409,70 e 1,80 kg. */
    static List<ItemRequest> carrinhoPadrao() {
        return List.of(CAMISETA, TENIS);
    }
}
