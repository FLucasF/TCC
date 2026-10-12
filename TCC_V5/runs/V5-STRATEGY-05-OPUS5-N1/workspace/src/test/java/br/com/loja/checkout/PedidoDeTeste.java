package br.com.loja.checkout;

import br.com.loja.checkout.pedido.ItemBruto;
import br.com.loja.checkout.pedido.PedidoBruto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Monta pedidos para os testes sem repetir os campos que não interessam. */
final class PedidoDeTeste {

    static final ItemBruto CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemBruto TENIS = item("Tenis", "249.90", 1, "1.20");

    private final List<ItemBruto> itens = new ArrayList<>();
    private String modalidadeEntrega = "ECONOMICA";
    private String cupom;
    private String formaPagamento = "PIX";
    private Integer parcelas;
    private String nivelClube = "BRONZE";
    private String regiao = "SUDESTE";

    static ItemBruto item(String nome, String preco, Integer quantidade, String peso) {
        return new ItemBruto(nome, preco == null ? null : new BigDecimal(preco), quantidade,
                peso == null ? null : new BigDecimal(peso));
    }

    static PedidoDeTeste pedido(ItemBruto... itens) {
        PedidoDeTeste pedido = new PedidoDeTeste();
        pedido.itens.addAll(List.of(itens));
        return pedido;
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

    PedidoBruto montar() {
        return new PedidoBruto(itens.isEmpty() ? List.of() : List.copyOf(itens),
                modalidadeEntrega, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }
}
