package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** A compra como o site envia: itens do carrinho e os codigos das opcoes escolhidas. */
public record Pedido(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    private static final int PARCELAS_PADRAO = 1;

    /** Carrinho vazio ou item sem preco, quantidade ou peso validos tornam o pedido invalido. */
    public List<Item> itensValidos() {
        if (itens == null || itens.isEmpty()
                || itens.stream().anyMatch(item -> item == null || !item.preenchido())) {
            throw new PedidoRecusado(Erro.PEDIDO_INVALIDO);
        }
        return itens;
    }

    public int parcelasEscolhidas() {
        return parcelas == null ? PARCELAS_PADRAO : parcelas;
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.emCentavos(itensValidos().stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal pesoKg() {
        return itensValidos().stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
