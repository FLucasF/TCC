package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que algum cupom pode precisar para decidir se aplica e quanto desconta.
 * A assinatura foi montada olhando o cupom mais exigente: MENOS50 olha o
 * subtotal, FRETEGRATIS olha o frete, LEVE3PAGUE2 olha os itens.
 */
public record CupomContexto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
}
