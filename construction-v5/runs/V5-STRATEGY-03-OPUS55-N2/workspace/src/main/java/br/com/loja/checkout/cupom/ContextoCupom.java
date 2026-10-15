package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** O que um cupom pode consultar: o carrinho e o frete que aparece no resumo. */
public record ContextoCupom(Carrinho carrinho, BigDecimal subtotalProdutos, BigDecimal frete) {
}
