package com.loja.checkout;

import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoRequest.ItemRequest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Monta pedidos para os testes sem repetir a lista de campos. */
final class ResumoFixture {

    static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private List<ItemRequest> itens = List.of(CAMISETA, TENIS);
    private String modalidadeEntrega = "EXPRESSA";
    private String cupom;
    private String formaPagamento = "PIX";
    private Integer parcelas;
    private String nivelClube = "BRONZE";
    private String regiao = "SUDESTE";

    static ResumoFixture pedido() {
        return new ResumoFixture();
    }

    static ItemRequest item(String nome, String preco, Integer quantidade, String peso) {
        return new ItemRequest(nome, preco == null ? null : new BigDecimal(preco), quantidade,
                peso == null ? null : new BigDecimal(peso));
    }

    ResumoFixture itens(ItemRequest... itens) {
        this.itens = itens == null ? null : Arrays.asList(itens);
        return this;
    }

    ResumoFixture semItens() {
        this.itens = List.of();
        return this;
    }

    ResumoFixture entrega(String modalidadeEntrega) {
        this.modalidadeEntrega = modalidadeEntrega;
        return this;
    }

    ResumoFixture cupom(String cupom) {
        this.cupom = cupom;
        return this;
    }

    ResumoFixture pagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
        return this;
    }

    ResumoFixture parcelas(Integer parcelas) {
        this.parcelas = parcelas;
        return this;
    }

    ResumoFixture clube(String nivelClube) {
        this.nivelClube = nivelClube;
        return this;
    }

    ResumoFixture regiao(String regiao) {
        this.regiao = regiao;
        return this;
    }

    ResumoRequest montar() {
        return new ResumoRequest(itens, modalidadeEntrega, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }
}
