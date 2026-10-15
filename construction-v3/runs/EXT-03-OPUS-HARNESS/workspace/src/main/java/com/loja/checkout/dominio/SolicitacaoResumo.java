package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O pedido como o site manda, ainda sem validar. */
public record SolicitacaoResumo(
        List<ItemSolicitado> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public static final int PARCELAS_PADRAO = 1;

    public int parcelasOuPadrao() {
        return parcelas == null ? PARCELAS_PADRAO : parcelas;
    }

    public record ItemSolicitado(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {

        boolean valido() {
            return positivo(precoUnitario)
                    && quantidade != null && quantidade > 0
                    && positivo(pesoKg);
        }

        private static boolean positivo(BigDecimal valor) {
            return valor != null && valor.signum() > 0;
        }

        Item paraItem() {
            return new Item(nome, precoUnitario, quantidade, pesoKg);
        }
    }

    /** Carrinho vazio, ou algum item com preco, quantidade ou peso zero/negativo/ausente. */
    Pedido paraPedido() {
        if (itens == null || itens.isEmpty()
                || itens.stream().anyMatch(item -> item == null || !item.valido())) {
            throw new CheckoutInvalidoException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(ItemSolicitado::paraItem).toList());
    }
}
