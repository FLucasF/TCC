package br.com.loja.pedidos.checkout;

import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;

/** Item do carrinho com o preço e o peso do catálogo no momento da compra. */
public record ItemPrecificado(String sku, int quantidade, BigDecimal precoUnitario, BigDecimal pesoKgUnitario) {

    public BigDecimal valor() {
        return Dinheiro.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public BigDecimal pesoKg() {
        return pesoKgUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
