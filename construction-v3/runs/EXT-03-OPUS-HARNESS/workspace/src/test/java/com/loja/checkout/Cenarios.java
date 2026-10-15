package com.loja.checkout;

import com.loja.checkout.dominio.SolicitacaoResumo;
import com.loja.checkout.dominio.SolicitacaoResumo.ItemSolicitado;

import java.math.BigDecimal;
import java.util.List;

/** Montagem dos pedidos usados nos testes. */
final class Cenarios {

    static final ItemSolicitado CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemSolicitado TENIS = item("Tenis", "249.90", 1, "1.20");
    static final ItemSolicitado FONE = item("Fone", "199.90", 2, "0.25");
    static final ItemSolicitado MEIA = item("Meia", "19.90", 7, "0.10");

    private Cenarios() {
    }

    static ItemSolicitado item(String nome, String preco, Integer quantidade, String peso) {
        return new ItemSolicitado(nome,
                preco == null ? null : new BigDecimal(preco),
                quantidade,
                peso == null ? null : new BigDecimal(peso));
    }

    static Pedido pedido(ItemSolicitado... itens) {
        return new Pedido(List.of(itens));
    }

    /** Builder enxuto para variar um campo por vez nos testes de erro. */
    record Pedido(List<ItemSolicitado> itens,
                  String entrega,
                  String cupom,
                  String pagamento,
                  Integer parcelas,
                  String clube,
                  String regiao) {

        Pedido(List<ItemSolicitado> itens) {
            this(itens, "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "SUDESTE");
        }

        Pedido itens(ItemSolicitado... valor) {
            return new Pedido(List.of(valor), entrega, cupom, pagamento, parcelas, clube, regiao);
        }

        Pedido entrega(String valor) {
            return new Pedido(itens, valor, cupom, pagamento, parcelas, clube, regiao);
        }

        Pedido cupom(String valor) {
            return new Pedido(itens, entrega, valor, pagamento, parcelas, clube, regiao);
        }

        Pedido pagamento(String valor) {
            return new Pedido(itens, entrega, cupom, valor, parcelas, clube, regiao);
        }

        Pedido parcelas(Integer valor) {
            return new Pedido(itens, entrega, cupom, pagamento, valor, clube, regiao);
        }

        Pedido clube(String valor) {
            return new Pedido(itens, entrega, cupom, pagamento, parcelas, valor, regiao);
        }

        Pedido regiao(String valor) {
            return new Pedido(itens, entrega, cupom, pagamento, parcelas, clube, valor);
        }

        SolicitacaoResumo solicitacao() {
            return new SolicitacaoResumo(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
        }
    }
}
