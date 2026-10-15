package com.loja.checkout;

import com.loja.checkout.api.CompraRequest;
import com.loja.checkout.api.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

/** Os itens e as compras usadas nos exemplos conferidos pelo financeiro. */
final class CompraDeExemplo {

    static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");
    static final ItemRequest FONE = item("Fone", "199.90", 2, "0.25");
    static final ItemRequest MEIA = item("Meia", "19.90", 7, "0.10");

    private CompraDeExemplo() {
    }

    static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    static CompraRequest compra(List<ItemRequest> itens, String entrega, String cupom,
                                String pagamento, Integer parcelas, String clube, String regiao) {
        return new CompraRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }
}
