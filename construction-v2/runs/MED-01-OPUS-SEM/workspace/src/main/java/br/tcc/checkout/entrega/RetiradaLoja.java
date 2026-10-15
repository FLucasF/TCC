package br.tcc.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;
import br.tcc.checkout.dominio.Pedido;

/** Cliente retira na loja: sem frete, disponível no dia seguinte. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

	@Override
	public String codigo() {
		return "RETIRADA_LOJA";
	}

	@Override
	public int prazoDias() {
		return 1;
	}

	@Override
	public BigDecimal calcularFrete(Pedido pedido) {
		return Dinheiro.ZERO;
	}
}
