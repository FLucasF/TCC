package br.tcc.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;

/** R$ 50,00 de desconto nos produtos, a partir de R$ 300,00 em produtos. */
@Component
public class CupomMenos50 implements Cupom {

	private static final BigDecimal DESCONTO = new BigDecimal("50.00");
	private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

	@Override
	public String codigo() {
		return "MENOS50";
	}

	@Override
	public BigDecimal calcularDesconto(ContextoCupom contexto) {
		return Dinheiro.arredondar(DESCONTO);
	}

	@Override
	public boolean aplicavel(ContextoCupom contexto) {
		return contexto.pedido().subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
	}
}
