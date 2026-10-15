package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** O que o cupom tem a disposicao para decidir e calcular o desconto. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return pedido.subtotalProdutos();
    }
}
