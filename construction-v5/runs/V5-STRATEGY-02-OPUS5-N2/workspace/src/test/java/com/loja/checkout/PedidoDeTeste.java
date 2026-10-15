package com.loja.checkout;

import com.loja.checkout.aplicacao.ItemRecebido;
import com.loja.checkout.aplicacao.PedidoRecebido;

import java.math.BigDecimal;
import java.util.List;

/** Monta pedidos nos testes sem repetir os campos que não interessam ao caso. */
final class PedidoDeTeste {

    private List<ItemRecebido> itens = List.of(item("Camiseta", "79.90", 2, "0.30"));
    private String modalidadeEntrega = "ECONOMICA";
    private String cupom;
    private String formaPagamento = "PIX";
    private Integer parcelas;
    private String nivelClube = "BRONZE";
    private String regiao = "SUDESTE";

    static PedidoDeTeste um() {
        return new PedidoDeTeste();
    }

    static ItemRecebido item(String nome, String preco, Integer quantidade, String peso) {
        return new ItemRecebido(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    PedidoDeTeste com(ItemRecebido... itens) {
        this.itens = List.of(itens);
        return this;
    }

    PedidoDeTeste semItens() {
        this.itens = List.of();
        return this;
    }

    PedidoDeTeste entrega(String modalidadeEntrega) {
        this.modalidadeEntrega = modalidadeEntrega;
        return this;
    }

    PedidoDeTeste cupom(String cupom) {
        this.cupom = cupom;
        return this;
    }

    PedidoDeTeste pagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
        return this;
    }

    PedidoDeTeste parcelas(Integer parcelas) {
        this.parcelas = parcelas;
        return this;
    }

    PedidoDeTeste clube(String nivelClube) {
        this.nivelClube = nivelClube;
        return this;
    }

    PedidoDeTeste regiao(String regiao) {
        this.regiao = regiao;
        return this;
    }

    PedidoRecebido pedido() {
        return new PedidoRecebido(itens, modalidadeEntrega, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }
}
