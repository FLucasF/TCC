package com.loja.checkout;

import com.loja.checkout.contrato.ItemRequest;
import com.loja.checkout.contrato.PedidoRequest;
import java.math.BigDecimal;
import java.util.List;

/** Atalhos para montar pedidos nos testes. */
final class Pedidos {

    static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private Pedidos() {
    }

    static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    static PedidoRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
                                String pagamento, Integer parcelas, String clube, String regiao) {
        return new PedidoRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }
}
