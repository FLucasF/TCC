package br.tcc.checkout.entrega;

import java.math.BigDecimal;

import br.tcc.checkout.dominio.Dinheiro;
import br.tcc.checkout.dominio.Pedido;

/** Base para modalidades que cobram uma taxa fixa mais um valor por kg do pedido. */
public abstract class EntregaPorPeso implements ModalidadeEntrega {

	private final BigDecimal taxaFixa;
	private final BigDecimal valorPorKg;

	protected EntregaPorPeso(String taxaFixa, String valorPorKg) {
		this.taxaFixa = new BigDecimal(taxaFixa);
		this.valorPorKg = new BigDecimal(valorPorKg);
	}

	@Override
	public BigDecimal calcularFrete(Pedido pedido) {
		return Dinheiro.arredondar(taxaFixa.add(valorPorKg.multiply(pedido.pesoTotalKg())));
	}
}
