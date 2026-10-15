package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** O que um cupom pode usar para decidir o desconto. {@code frete} é o frete já cobrado do cliente. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {
}
