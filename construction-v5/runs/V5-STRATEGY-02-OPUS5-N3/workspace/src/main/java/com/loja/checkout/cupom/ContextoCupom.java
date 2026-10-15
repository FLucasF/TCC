package com.loja.checkout.cupom;

import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que um cupom pode precisar para decidir: os itens (LEVE3PAGUE2), o
 * subtotal (MENOS50, BEMVINDO10) e o frete já calculado (FRETEGRATIS).
 */
public record ContextoCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
