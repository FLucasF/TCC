package com.loja.checkout.pedido;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;

import java.math.BigDecimal;
import java.util.List;

public class Pedido {

    private final List<ItemPedido> itens;

    private Pedido(List<ItemPedido> itens) {
        this.itens = itens;
    }

    public static Pedido criar(List<ItemBruto> itensBrutos) {
        if (itensBrutos == null || itensBrutos.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itens = itensBrutos.stream().map(Pedido::validar).toList();
        return new Pedido(itens);
    }

    private static ItemPedido validar(ItemBruto item) {
        if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    public List<ItemPedido> itens() {
        return itens;
    }

    public BigDecimal subtotalProdutos() {
        return itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
