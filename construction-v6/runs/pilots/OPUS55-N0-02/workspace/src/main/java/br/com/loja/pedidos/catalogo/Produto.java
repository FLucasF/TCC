package br.com.loja.pedidos.catalogo;

import java.math.BigDecimal;

public record Produto(String sku, String nome, BigDecimal preco, BigDecimal pesoKg, int estoque, boolean foraDeLinha) {
}
