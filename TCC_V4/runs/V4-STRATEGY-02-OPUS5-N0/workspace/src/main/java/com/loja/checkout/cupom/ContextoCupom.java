package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/**
 * Dados do pedido disponíveis para o cupom decidir o desconto.
 * O frete é o que vai aparecer no resumo (já zerado quando o cliente OURO não paga).
 */
public record ContextoCupom(Carrinho carrinho, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return carrinho.subtotalProdutos();
    }
}
