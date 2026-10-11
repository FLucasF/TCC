package com.loja.checkout.cupom;

import com.loja.checkout.comum.Carrinho;
import java.math.BigDecimal;

/** O que um cupom pode consultar: o carrinho e o frete já calculado (como aparece no resumo). */
public record ContextoCupom(Carrinho carrinho, BigDecimal frete) {
}
