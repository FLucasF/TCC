package br.tcc.checkout.cupom;

import br.tcc.checkout.dominio.DadosPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisCupom implements Cupom {

	@Override
	public String getCodigo() {
		return "FRETEGRATIS";
	}

	@Override
	public boolean isAplicavel(DadosPedido pedido) {
		return true;
	}

	@Override
	public BigDecimal calcularDesconto(DadosPedido pedido, BigDecimal frete) {
		return frete;
	}
}
