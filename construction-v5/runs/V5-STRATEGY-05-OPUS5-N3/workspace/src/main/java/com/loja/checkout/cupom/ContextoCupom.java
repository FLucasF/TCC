package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

/** Tudo o que algum cupom precisa olhar para decidir e para calcular. */
public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
