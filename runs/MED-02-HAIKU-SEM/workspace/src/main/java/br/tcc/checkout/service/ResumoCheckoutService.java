package br.tcc.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.tcc.checkout.model.FormaPagamento;
import br.tcc.checkout.model.ItemCarrinho;
import br.tcc.checkout.model.ModalidadeEntrega;
import br.tcc.checkout.model.RequisicaoResumo;
import br.tcc.checkout.model.RespostaResumo;
import br.tcc.checkout.util.ArredondamentoUtil;

@Service
public class ResumoCheckoutService {

	private static final Map<String, CupomInfo> CUPONS = new HashMap<>();

	static {
		CUPONS.put("BEMVINDO10", new CupomInfo("BEMVINDO10", "PERCENTUAL", 10.0, 0.0, null));
		CUPONS.put("MENOS50", new CupomInfo("MENOS50", "FIXO", 50.0, 300.0, null));
		CUPONS.put("FRETEGRATIS", new CupomInfo("FRETEGRATIS", "FRETE", 0.0, 0.0, null));
		CUPONS.put("LEVE3PAGUE2", new CupomInfo("LEVE3PAGUE2", "ITEM", 0.0, 0.0, null));
	}

	public String validarRequisicao(RequisicaoResumo requisicao) {
		if (!validarItens(requisicao.getItens())) {
			return "PEDIDO_INVALIDO";
		}

		if (requisicao.getModalidadeEntrega() == null
				|| ModalidadeEntrega.fromString(requisicao.getModalidadeEntrega()) == null) {
			return "MODALIDADE_INVALIDA";
		}

		if (requisicao.getFormaPagamento() == null
				|| FormaPagamento.fromString(requisicao.getFormaPagamento()) == null) {
			return "FORMA_PAGAMENTO_INVALIDA";
		}

		Integer parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;

		FormaPagamento forma = FormaPagamento.fromString(requisicao.getFormaPagamento());
		if (!validarParcelas(forma, parcelas)) {
			return "PARCELAMENTO_INVALIDO";
		}

		Double subtotal = calcularSubtotalProdutos(requisicao.getItens());
		Double desconto = calcularDescontoCupomComItens(requisicao.getCupom(),
				requisicao.getItens());

		if (desconto == null) {
			if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
				return "CUPOM_INVALIDO";
			}
		} else if (desconto == -1.0) {
			return "CUPOM_NAO_APLICAVEL";
		}

		ModalidadeEntrega modalidade = ModalidadeEntrega
				.fromString(requisicao.getModalidadeEntrega());
		Double pesoTotal = calcularPesoTotal(requisicao.getItens());

		if (!validarModalidadeParaPeso(modalidade, pesoTotal)) {
			return "MODALIDADE_INDISPONIVEL";
		}

		Double freteCalculado = calcularFrete(modalidade, pesoTotal, requisicao.getCupom(),
				subtotal);
		Double totalPedido = subtotal - (desconto != null && desconto >= 0 ? desconto : 0)
				+ freteCalculado;

		if (!validarFormaPagamento(forma, totalPedido)) {
			return "FORMA_PAGAMENTO_INDISPONIVEL";
		}

		return null;
	}

	public RespostaResumo calcularResumo(RequisicaoResumo requisicao) {
		double subtotalProdutos = calcularSubtotalProdutos(requisicao.getItens());
		double pesoTotal = calcularPesoTotal(requisicao.getItens());

		double descontoCupom = 0.0;
		if (requisicao.getCupom() != null && !requisicao.getCupom().isEmpty()) {
			Double desconto = calcularDescontoCupomComItens(requisicao.getCupom(),
					requisicao.getItens());
			if (desconto != null && desconto >= 0) {
				descontoCupom = desconto;
			}
		}

		ModalidadeEntrega modalidade = ModalidadeEntrega
				.fromString(requisicao.getModalidadeEntrega());
		double frete = calcularFrete(modalidade, pesoTotal, requisicao.getCupom(),
				subtotalProdutos);

		double totalPedido = subtotalProdutos - descontoCupom + frete;

		FormaPagamento forma = FormaPagamento.fromString(requisicao.getFormaPagamento());
		Integer parcelas = requisicao.getParcelas() != null ? requisicao.getParcelas() : 1;

		double ajuste = calcularAjustePagamento(forma, totalPedido, parcelas);
		double totalFinal = totalPedido + ajuste;

		double valorParcela = calcularValorParcela(forma, totalFinal, parcelas);

		return new RespostaResumo(
				ArredondamentoUtil.arredondarParaCentavos(subtotalProdutos),
				ArredondamentoUtil.arredondarParaCentavos(descontoCupom),
				ArredondamentoUtil.arredondarParaCentavos(frete),
				modalidade.getPrazoEntregaDias(),
				ArredondamentoUtil.arredondarParaCentavos(ajuste),
				ArredondamentoUtil.arredondarParaCentavos(totalFinal),
				parcelas,
				ArredondamentoUtil.arredondarParaCentavos(valorParcela)
		);
	}

	private boolean validarItens(List<ItemCarrinho> itens) {
		if (itens == null || itens.isEmpty()) {
			return false;
		}

		for (ItemCarrinho item : itens) {
			if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0
					|| item.getQuantidade() == null || item.getQuantidade() <= 0
					|| item.getPesoKg() == null || item.getPesoKg() < 0) {
				return false;
			}
		}

		return true;
	}

	private double calcularSubtotalProdutos(List<ItemCarrinho> itens) {
		double subtotal = 0.0;
		for (ItemCarrinho item : itens) {
			double itemTotal = item.getPrecoUnitario() * item.getQuantidade();
			subtotal = ArredondamentoUtil.arredondarParaCentavos(subtotal + itemTotal);
		}
		return subtotal;
	}

	private double calcularPesoTotal(List<ItemCarrinho> itens) {
		double pesoTotal = 0.0;
		for (ItemCarrinho item : itens) {
			pesoTotal += item.getPesoKg() * item.getQuantidade();
		}
		return pesoTotal;
	}

	private Double calcularDescontoCupom(String codigosCupom, double subtotalProdutos) {
		if (codigosCupom == null || codigosCupom.isEmpty()) {
			return 0.0;
		}

		CupomInfo cupom = CUPONS.get(codigosCupom);
		if (cupom == null) {
			return null;
		}

		if ("PERCENTUAL".equals(cupom.tipo)) {
			double desconto = subtotalProdutos * (cupom.valor / 100.0);
			return ArredondamentoUtil.arredondarParaCentavos(desconto);
		} else if ("FIXO".equals(cupom.tipo)) {
			if (subtotalProdutos < cupom.valorMinimo) {
				return -1.0;
			}
			return ArredondamentoUtil.arredondarParaCentavos(cupom.valor);
		} else if ("FRETE".equals(cupom.tipo)) {
			return 0.0;
		} else if ("ITEM".equals(cupom.tipo)) {
			return 0.0;
		}

		return 0.0;
	}

	public Double calcularDescontoCupomComItens(String codigosCupom, List<ItemCarrinho> itens) {
		if (codigosCupom == null || codigosCupom.isEmpty()) {
			return 0.0;
		}

		if (!"LEVE3PAGUE2".equals(codigosCupom)) {
			double subtotal = calcularSubtotalProdutos(itens);
			return calcularDescontoCupom(codigosCupom, subtotal);
		}

		double desconto = 0.0;
		for (ItemCarrinho item : itens) {
			int quantidadeGratis = item.getQuantidade() / 3;
			double descontoItem = quantidadeGratis * item.getPrecoUnitario();
			desconto = ArredondamentoUtil.arredondarParaCentavos(desconto + descontoItem);
		}

		return desconto;
	}

	private double calcularFrete(ModalidadeEntrega modalidade, double pesoTotal, String cupom,
			double subtotalProdutos) {
		double frete = modalidade.calcularFrete(pesoTotal);
		frete = ArredondamentoUtil.arredondarParaCentavos(frete);

		if ("FRETEGRATIS".equals(cupom)) {
			return 0.0;
		}

		return frete;
	}

	private boolean validarModalidadeParaPeso(ModalidadeEntrega modalidade, double pesoTotal) {
		if (modalidade == ModalidadeEntrega.MOTOBOY) {
			return pesoTotal <= 5.0;
		}
		return true;
	}

	private boolean validarFormaPagamento(FormaPagamento forma, double total) {
		if (forma == FormaPagamento.BOLETO) {
			return total <= 1000.0;
		}
		return true;
	}

	private boolean validarParcelas(FormaPagamento forma, int parcelas) {
		if (forma == FormaPagamento.CARTAO) {
			return parcelas >= 1 && parcelas <= 12;
		} else if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
			return parcelas == 1;
		}
		return false;
	}

	private double calcularAjustePagamento(FormaPagamento forma, double totalPedido,
			int parcelas) {
		if (forma == FormaPagamento.PIX) {
			double desconto = totalPedido * 0.05;
			return -ArredondamentoUtil.arredondarParaCentavos(desconto);
		} else if (forma == FormaPagamento.BOLETO) {
			return ArredondamentoUtil.arredondarParaCentavos(3.49);
		} else if (forma == FormaPagamento.CARTAO) {
			if (parcelas <= 3) {
				return 0.0;
			}
			double taxaMensal = 0.0199;
			double numerador = totalPedido * taxaMensal;
			double denominador = 1.0 - Math.pow(1.0 + taxaMensal, -parcelas);
			double parcelaComJuro = numerador / denominador;
			double parcelaArredondada = ArredondamentoUtil.arredondarParaCentavos(parcelaComJuro);
			double totalComJuro = parcelaArredondada * parcelas;
			double juro = totalComJuro - totalPedido;
			return ArredondamentoUtil.arredondarParaCentavos(juro);
		}
		return 0.0;
	}

	private double calcularValorParcela(FormaPagamento forma, double totalFinal, int parcelas) {
		double parcela = totalFinal / parcelas;
		return ArredondamentoUtil.arredondarParaCentavos(parcela);
	}

	private static class CupomInfo {
		String codigo;
		String tipo;
		double valor;
		double valorMinimo;
		Integer maxParcelas;

		CupomInfo(String codigo, String tipo, double valor, double valorMinimo,
				Integer maxParcelas) {
			this.codigo = codigo;
			this.tipo = tipo;
			this.valor = valor;
			this.valorMinimo = valorMinimo;
			this.maxParcelas = maxParcelas;
		}
	}
}
