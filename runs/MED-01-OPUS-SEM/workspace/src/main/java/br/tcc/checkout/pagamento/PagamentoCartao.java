package br.tcc.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;

/** Cartão de crédito: até 3x sem juros, de 4x a 12x com 1,99% ao mês (tabela Price). */
@Component
public class PagamentoCartao implements FormaPagamento {

	private static final int MAXIMO_PARCELAS = 12;
	private static final int MAXIMO_SEM_JUROS = 3;
	private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

	@Override
	public String codigo() {
		return "CARTAO";
	}

	@Override
	public boolean aceitaParcelas(int parcelas) {
		return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
	}

	@Override
	public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
		if (parcelas <= MAXIMO_SEM_JUROS) {
			BigDecimal totalFinal = Dinheiro.arredondar(totalPedido);
			BigDecimal valorParcela = Dinheiro.arredondar(
					totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
			return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
		}
		BigDecimal valorParcela = Dinheiro.arredondar(parcelaPrice(totalPedido, parcelas));
		BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
		return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
	}

	/** parcela = total × taxa ÷ (1 − (1 + taxa)^−parcelas) */
	private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
		BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CALCULO);
		BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.CALCULO));
		return totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CALCULO);
	}
}
