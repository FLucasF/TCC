package br.tcc.checkout.pagamento;

import br.tcc.checkout.exception.CheckoutErrorCode;
import br.tcc.checkout.exception.CheckoutException;
import br.tcc.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class PagamentoService {

	private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
	private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
	private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
	private static final BigDecimal TAXA_JUROS_CARTAO_AO_MES = new BigDecimal("0.0199");
	private static final int PARCELAS_MAX_SEM_JUROS_CARTAO = 3;
	private static final int PARCELAS_MAX_CARTAO = 12;

	public void validarParcelamento(FormaPagamento forma, int parcelas) {
		switch (forma) {
			case PIX, BOLETO -> {
				if (parcelas != 1) {
					throw new CheckoutException(CheckoutErrorCode.PARCELAMENTO_INVALIDO);
				}
			}
			case CARTAO -> {
				if (parcelas < 1 || parcelas > PARCELAS_MAX_CARTAO) {
					throw new CheckoutException(CheckoutErrorCode.PARCELAMENTO_INVALIDO);
				}
			}
		}
	}

	public void validarDisponibilidade(FormaPagamento forma, BigDecimal totalPedido) {
		if (forma == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
			throw new CheckoutException(CheckoutErrorCode.FORMA_PAGAMENTO_INDISPONIVEL);
		}
	}

	public ResultadoPagamento calcular(FormaPagamento forma, BigDecimal totalPedido, int parcelas) {
		return switch (forma) {
			case PIX -> calcularPix(totalPedido);
			case BOLETO -> calcularBoleto(totalPedido);
			case CARTAO -> calcularCartao(totalPedido, parcelas);
		};
	}

	private ResultadoPagamento calcularPix(BigDecimal totalPedido) {
		BigDecimal ajuste = Dinheiro.arredondar(totalPedido.multiply(DESCONTO_PIX).negate());
		BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(ajuste));
		return new ResultadoPagamento(ajuste, totalFinal, 1, totalFinal);
	}

	private ResultadoPagamento calcularBoleto(BigDecimal totalPedido) {
		BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA_BOLETO));
		return new ResultadoPagamento(TARIFA_BOLETO, totalFinal, 1, totalFinal);
	}

	private ResultadoPagamento calcularCartao(BigDecimal totalPedido, int parcelas) {
		if (parcelas <= PARCELAS_MAX_SEM_JUROS_CARTAO) {
			BigDecimal valorParcela = Dinheiro
					.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
			return new ResultadoPagamento(new BigDecimal("0.00"), totalPedido, parcelas, valorParcela);
		}

		BigDecimal denominador = calcularDenominadorPrice(parcelas);
		BigDecimal valorParcela = Dinheiro.arredondar(
				totalPedido.multiply(TAXA_JUROS_CARTAO_AO_MES).divide(denominador, 10, RoundingMode.HALF_EVEN));
		BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
		BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
		return new ResultadoPagamento(ajuste, totalFinal, parcelas, valorParcela);
	}

	/**
	 * Denominador da fórmula da tabela Price: 1 - (1 + taxa)^-parcelas.
	 */
	private BigDecimal calcularDenominadorPrice(int parcelas) {
		BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_CARTAO_AO_MES);
		BigDecimal potencia = umMaisTaxa.pow(parcelas, new MathContext(20));
		BigDecimal elevadoNegativo = BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN);
		return BigDecimal.ONE.subtract(elevadoNegativo);
	}
}
