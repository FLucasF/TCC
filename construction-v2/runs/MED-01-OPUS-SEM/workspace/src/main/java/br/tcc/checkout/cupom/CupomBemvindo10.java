package br.tcc.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;

/** 10% de desconto no valor dos produtos. */
@Component
public class CupomBemvindo10 implements Cupom {

	private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

	@Override
	public String codigo() {
		return "BEMVINDO10";
	}

	@Override
	public BigDecimal calcularDesconto(ContextoCupom contexto) {
		return Dinheiro.arredondar(contexto.pedido().subtotalProdutos().multiply(PERCENTUAL));
	}
}
