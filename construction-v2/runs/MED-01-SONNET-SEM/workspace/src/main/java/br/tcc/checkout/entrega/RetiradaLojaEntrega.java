package br.tcc.checkout.entrega;

import br.tcc.checkout.dominio.DadosPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaEntrega implements OpcaoEntrega {

	@Override
	public String getCodigo() {
		return "RETIRADA_LOJA";
	}

	@Override
	public int getPrazoDias() {
		return 1;
	}

	@Override
	public boolean isDisponivel(DadosPedido pedido) {
		return true;
	}

	@Override
	public BigDecimal calcularFrete(DadosPedido pedido) {
		return BigDecimal.ZERO;
	}
}
