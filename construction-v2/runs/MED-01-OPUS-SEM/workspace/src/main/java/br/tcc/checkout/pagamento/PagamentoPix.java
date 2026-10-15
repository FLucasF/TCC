package br.tcc.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;

/** Pix à vista, com 5% de desconto no total do pedido. */
@Component
public class PagamentoPix implements FormaPagamento {

	private static final BigDecimal DESCONTO = new BigDecimal("0.05");

	@Override
	public String codigo() {
		return "PIX";
	}

	@Override
	public boolean aceitaParcelas(int parcelas) {
		return parcelas == 1;
	}

	@Override
	public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
		BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(DESCONTO));
		BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
		return new ResultadoPagamento(totalFinal, parcelas, totalFinal);
	}
}
