package com.loja.checkout;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoRequest.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

/** Atalhos para montar pedidos nos testes. */
final class Pedidos {

    static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");
    static final ItemRequest FONE = item("Fone", "199.90", 2, "0.25");
    static final ItemRequest MEIA = item("Meia", "19.90", 7, "0.10");

    private Pedidos() {
    }

    static ItemRequest item(String nome, String preco, Integer quantidade, String peso) {
        return new ItemRequest(nome, preco == null ? null : new BigDecimal(preco), quantidade,
                peso == null ? null : new BigDecimal(peso));
    }

    static Builder pedido(ItemRequest... itens) {
        return new Builder(List.of(itens));
    }

    static final class Builder {
        private final List<ItemRequest> itens;
        private String modalidadeEntrega = "RETIRADA_LOJA";
        private String cupom;
        private String formaPagamento = "PIX";
        private Integer parcelas;
        private String nivelClube = "BRONZE";
        private String regiao = "SUDESTE";

        private Builder(List<ItemRequest> itens) {
            this.itens = itens;
        }

        Builder entrega(String modalidade) {
            this.modalidadeEntrega = modalidade;
            return this;
        }

        Builder cupom(String cupom) {
            this.cupom = cupom;
            return this;
        }

        Builder pagamento(String forma) {
            this.formaPagamento = forma;
            return this;
        }

        Builder parcelas(Integer parcelas) {
            this.parcelas = parcelas;
            return this;
        }

        Builder clube(String nivel) {
            this.nivelClube = nivel;
            return this;
        }

        Builder regiao(String regiao) {
            this.regiao = regiao;
            return this;
        }

        ResumoRequest montar() {
            return new ResumoRequest(itens, modalidadeEntrega, cupom, formaPagamento, parcelas,
                    nivelClube, regiao);
        }
    }
}
