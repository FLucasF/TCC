package br.tcc.checkout.cupom;

import br.tcc.checkout.api.dto.ItemPedidoRequest;
import br.tcc.checkout.dominio.DadosPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2Cupom implements Cupom {

	private static final int TAMANHO_LEVA = 3;

	@Override
	public String getCodigo() {
		return "LEVE3PAGUE2";
	}

	@Override
	public boolean isAplicavel(DadosPedido pedido) {
		return true;
	}

	@Override
	public BigDecimal calcularDesconto(DadosPedido pedido, BigDecimal frete) {
		BigDecimal desconto = BigDecimal.ZERO;
		for (ItemPedidoRequest item : pedido.itens()) {
			int unidadesGratis = item.quantidade() / TAMANHO_LEVA;
			if (unidadesGratis > 0) {
				desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
			}
		}
		return desconto;
	}
}
