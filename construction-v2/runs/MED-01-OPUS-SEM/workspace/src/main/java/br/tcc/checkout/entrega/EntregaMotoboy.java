package br.tcc.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;
import br.tcc.checkout.dominio.Pedido;

/** R$ 18,00 fixos no mesmo dia, para pedidos de até 5 kg. */
@Component
public class EntregaMotoboy implements ModalidadeEntrega {

	private static final BigDecimal TAXA = new BigDecimal("18.00");
	private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

	@Override
	public String codigo() {
		return "MOTOBOY";
	}

	@Override
	public int prazoDias() {
		return 0;
	}

	@Override
	public BigDecimal calcularFrete(Pedido pedido) {
		return Dinheiro.arredondar(TAXA);
	}

	@Override
	public boolean atende(Pedido pedido) {
		return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
	}
}
