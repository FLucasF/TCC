package br.tcc.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;
import br.tcc.checkout.dominio.ItemPedido;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graça. */
@Component
public class CupomLeve3Pague2 implements Cupom {

	private static final int LOTE = 3;

	@Override
	public String codigo() {
		return "LEVE3PAGUE2";
	}

	@Override
	public BigDecimal calcularDesconto(ContextoCupom contexto) {
		BigDecimal desconto = BigDecimal.ZERO;
		for (ItemPedido item : contexto.pedido().itens()) {
			int gratis = item.quantidade() / LOTE;
			desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
		}
		return Dinheiro.arredondar(desconto);
	}
}
