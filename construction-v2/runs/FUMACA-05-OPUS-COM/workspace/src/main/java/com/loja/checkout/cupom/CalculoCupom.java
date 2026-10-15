package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que um cupom pode precisar olhar: os itens (LEVE3PAGUE2), o subtotal
 * (BEMVINDO10, MENOS50) e o frete ja calculado (FRETEGRATIS).
 */
public record CalculoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
