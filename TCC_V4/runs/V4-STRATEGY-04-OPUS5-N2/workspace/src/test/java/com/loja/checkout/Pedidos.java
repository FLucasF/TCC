package com.loja.checkout;

import com.loja.checkout.dominio.ItemRecebido;
import java.math.BigDecimal;

/** Atalhos para montar os itens dos pedidos nos testes. */
final class Pedidos {

    static final ItemRecebido CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemRecebido TENIS = item("Tenis", "249.90", 1, "1.20");
    static final ItemRecebido FONE = item("Fone", "199.90", 2, "0.25");
    static final ItemRecebido MEIA = item("Meia", "19.90", 7, "0.10");

    private Pedidos() {
    }

    static ItemRecebido item(String nome, String preco, int quantidade, String pesoKg) {
        return new ItemRecebido(nome, new BigDecimal(preco), quantidade, new BigDecimal(pesoKg));
    }
}
