package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** O que um cupom pode olhar para decidir se vale e quanto abate. */
public record ContextoCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
}
