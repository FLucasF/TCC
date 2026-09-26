package br.tcc.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;

/** Boleto à vista, com tarifa bancária de R$ 3,49 e teto de R$ 1.000,00 no total do pedido. */
@Component
public class PagamentoBoleto implements FormaPagamento {

	private static final BigDecimal TARIFA = new BigDecimal("3.49");
	private static final BigDecimal TETO_TOTAL = new BigDecimal("1000.00");

	@Override
	public String codigo() {
		return "BOLETO";
	}

	@Override
	public boolean aceitaParcelas(int parcelas) {
		return parcelas == 1;
	}

	@Override
	public boolean atende(BigDecimal totalPedido) {
		return totalPedido.compareTo(TETO_TOTAL) <= 0;
	}

	@Override
	public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
		BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA));
		return new ResultadoPagamento(totalFinal, parcelas, totalFinal);
	}
}
