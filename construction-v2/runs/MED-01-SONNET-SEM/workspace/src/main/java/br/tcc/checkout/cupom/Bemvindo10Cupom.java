package br.tcc.checkout.cupom;

import br.tcc.checkout.dominio.DadosPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10Cupom implements Cupom {

	private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

	@Override
	public String getCodigo() {
		return "BEMVINDO10";
	}

	@Override
	public boolean isAplicavel(DadosPedido pedido) {
		return true;
	}

	@Override
	public BigDecimal calcularDesconto(DadosPedido pedido, BigDecimal frete) {
		return pedido.subtotalProdutos().multiply(PERCENTUAL);
	}
}
