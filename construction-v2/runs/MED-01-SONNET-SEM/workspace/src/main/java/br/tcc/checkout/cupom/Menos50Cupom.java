package br.tcc.checkout.cupom;

import br.tcc.checkout.dominio.DadosPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50Cupom implements Cupom {

	private static final BigDecimal DESCONTO = new BigDecimal("50.00");
	private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

	@Override
	public String getCodigo() {
		return "MENOS50";
	}

	@Override
	public boolean isAplicavel(DadosPedido pedido) {
		return pedido.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
	}

	@Override
	public BigDecimal calcularDesconto(DadosPedido pedido, BigDecimal frete) {
		return DESCONTO;
	}
}
