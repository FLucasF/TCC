package com.loja.checkout.cupom;

import com.loja.checkout.core.ItemValidado;
import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo o que um cupom pode precisar para decidir se vale e quanto desconta.
 * A assinatura atende o caso mais exigente: LEVE3PAGUE2 precisa dos itens e
 * FRETEGRATIS precisa do frete já calculado.
 */
public record CupomContexto(BigDecimal subtotalProdutos, List<ItemValidado> itens, BigDecimal frete) {
}
