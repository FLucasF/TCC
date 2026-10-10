package com.loja.checkout.domain.cupom;

import com.loja.checkout.api.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo de que um cupom pode precisar para decidir e calcular o desconto. A
 * assinatura atende o cupom mais exigente: FRETEGRATIS olha o frete, MENOS50
 * olha o subtotal e LEVE3PAGUE2 olha os itens.
 */
public record CupomContexto(
        BigDecimal subtotalProdutos,
        BigDecimal frete,
        List<ItemCarrinho> itens) {
}
