package br.tcc.checkout.entrega;

import br.tcc.checkout.dominio.DadosPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpressaEntrega implements OpcaoEntrega {

	private static final BigDecimal TAXA_BASE = new BigDecimal("25.00");
	private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

	@Override
	public String getCodigo() {
		return "EXPRESSA";
	}

	@Override
	public int getPrazoDias() {
		return 2;
	}

	@Override
	public boolean isDisponivel(DadosPedido pedido) {
		return true;
	}

	@Override
	public BigDecimal calcularFrete(DadosPedido pedido) {
		return TAXA_BASE.add(TAXA_POR_KG.multiply(pedido.pesoTotalKg()));
	}
}
