package com.loja.checkout.cupom;

import com.loja.checkout.dominio.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Comportamento de um cupom. Cada cupom tem regra de aplicabilidade e fórmula de
 * desconto próprias (não é só um dado que muda, a conta inteira muda de cupom para
 * cupom), por isso cada um mora numa implementação isolada, escolhida por lookup num
 * mapa (ver {@link CupomRegistry}).
 *
 * A assinatura de calcularDesconto foi decidida olhando o caso mais exigente: o
 * LEVE3PAGUE2 precisa da lista de itens (desconto por item) e o FRETEGRATIS precisa do
 * valor do frete já considerando a isenção do clube.
 */
public interface CupomCalculadora {

    String getCodigo();

    boolean aplicavel(BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal freteAposClube);
}
