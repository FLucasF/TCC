package com.loja.checkout;

import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Monta pedidos nos testes sem repetir os campos que nao interessam ao caso. */
final class PedidoBuilder {

    private List<Item> itens = List.of(camiseta(2), tenis(1));
    private String modalidadeEntrega = "ECONOMICA";
    private String cupom;
    private String formaPagamento = "PIX";
    private Integer parcelas;
    private String nivelClube = "BRONZE";
    private String regiao = "SUDESTE";

    static PedidoBuilder pedido() {
        return new PedidoBuilder();
    }

    static Item item(String nome, String preco, int quantidade, String pesoKg) {
        return new Item(nome, new BigDecimal(preco), quantidade, new BigDecimal(pesoKg));
    }

    static Item camiseta(int quantidade) {
        return item("Camiseta", "79.90", quantidade, "0.30");
    }

    static Item tenis(int quantidade) {
        return item("Tenis", "249.90", quantidade, "1.20");
    }

    static Item fone(int quantidade) {
        return item("Fone", "199.90", quantidade, "0.25");
    }

    PedidoBuilder itens(Item... itens) {
        this.itens = itens == null ? null : Arrays.asList(itens);
        return this;
    }

    PedidoBuilder entrega(String modalidadeEntrega) {
        this.modalidadeEntrega = modalidadeEntrega;
        return this;
    }

    PedidoBuilder cupom(String cupom) {
        this.cupom = cupom;
        return this;
    }

    PedidoBuilder pagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
        return this;
    }

    PedidoBuilder parcelas(Integer parcelas) {
        this.parcelas = parcelas;
        return this;
    }

    PedidoBuilder clube(String nivelClube) {
        this.nivelClube = nivelClube;
        return this;
    }

    PedidoBuilder regiao(String regiao) {
        this.regiao = regiao;
        return this;
    }

    Pedido construir() {
        return new Pedido(itens, modalidadeEntrega, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }
}
