package br.tcc.checkout.entrega;

import br.tcc.checkout.dominio.DadosPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyEntrega implements OpcaoEntrega {

	private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
	private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

	@Override
	public String getCodigo() {
		return "MOTOBOY";
	}

	@Override
	public int getPrazoDias() {
		return 0;
	}

	@Override
	public boolean isDisponivel(DadosPedido pedido) {
		return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
	}

	@Override
	public BigDecimal calcularFrete(DadosPedido pedido) {
		return TAXA_FIXA;
	}
}
