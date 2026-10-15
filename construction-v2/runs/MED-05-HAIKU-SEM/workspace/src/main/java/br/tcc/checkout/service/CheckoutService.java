package br.tcc.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaResumo;

@Service
public class CheckoutService {

	private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
	private static final BigDecimal PIX_DESCONTO = new BigDecimal("0.05");
	private static final BigDecimal BOLETO_TARIFA = new BigDecimal("3.49");
	private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
	private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
	private static final int LIMITE_MOTOBOY_KG = 5;

	private static final Map<String, EntregaConfig> MODALIDADES_ENTREGA = new HashMap<>();
	private static final Map<String, CupomConfig> CUPONS = new HashMap<>();

	static {
		MODALIDADES_ENTREGA.put("ECONOMICA", new EntregaConfig(new BigDecimal("12.00"), new BigDecimal("2.00"), 7));
		MODALIDADES_ENTREGA.put("EXPRESSA", new EntregaConfig(new BigDecimal("25.00"), new BigDecimal("4.50"), 2));
		MODALIDADES_ENTREGA.put("RETIRADA_LOJA", new EntregaConfig(BigDecimal.ZERO, BigDecimal.ZERO, 1));
		MODALIDADES_ENTREGA.put("MOTOBOY", new EntregaConfig(new BigDecimal("18.00"), BigDecimal.ZERO, 0));

		CUPONS.put("BEMVINDO10", new CupomConfig("BEMVINDO10"));
		CUPONS.put("MENOS50", new CupomConfig("MENOS50"));
		CUPONS.put("FRETEGRATIS", new CupomConfig("FRETEGRATIS"));
		CUPONS.put("LEVE3PAGUE2", new CupomConfig("LEVE3PAGUE2"));
	}

	public String validarRequisicao(RequisicaoResumo req) {
		if (req.getItens() == null || req.getItens().isEmpty()) {
			return "PEDIDO_INVALIDO";
		}

		for (ItemCarrinho item : req.getItens()) {
			if (item.getPrecoUnitario() == null || item.getQuantidade() == null || item.getPesoKg() == null) {
				return "PEDIDO_INVALIDO";
			}

			if (item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0
					|| item.getQuantidade() <= 0
					|| item.getPesoKg().compareTo(BigDecimal.ZERO) < 0) {
				return "PEDIDO_INVALIDO";
			}
		}

		if (req.getModalidadeEntrega() == null) {
			return "MODALIDADE_INVALIDA";
		}

		if (!MODALIDADES_ENTREGA.containsKey(req.getModalidadeEntrega())) {
			return "MODALIDADE_INVALIDA";
		}

		String cupomErro = validarCupom(req);
		if (cupomErro != null) {
			return cupomErro;
		}

		if (req.getFormaPagamento() == null) {
			return "FORMA_PAGAMENTO_INVALIDA";
		}

		if (!("PIX".equals(req.getFormaPagamento()) || "CARTAO".equals(req.getFormaPagamento())
				|| "BOLETO".equals(req.getFormaPagamento()))) {
			return "FORMA_PAGAMENTO_INVALIDA";
		}

		String parcelasErro = validarParcelas(req);
		if (parcelasErro != null) {
			return parcelasErro;
		}

		return null;
	}

	private String validarCupom(RequisicaoResumo req) {
		if (req.getCupom() == null) {
			return null;
		}

		if (!CUPONS.containsKey(req.getCupom())) {
			return "CUPOM_INVALIDO";
		}

		BigDecimal subtotal = calcularSubtotalProdutos(req.getItens());

		if ("MENOS50".equals(req.getCupom())) {
			if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
				return "CUPOM_NAO_APLICAVEL";
			}
		}

		return null;
	}

	private String validarParcelas(RequisicaoResumo req) {
		Integer parcelas = req.getParcelas() != null ? req.getParcelas() : 1;

		if ("PIX".equals(req.getFormaPagamento()) || "BOLETO".equals(req.getFormaPagamento())) {
			if (parcelas != 1) {
				return "PARCELAMENTO_INVALIDO";
			}
		} else if ("CARTAO".equals(req.getFormaPagamento())) {
			if (parcelas < 1 || parcelas > 12) {
				return "PARCELAMENTO_INVALIDO";
			}
		}

		return null;
	}

	public String validarDisponibilidade(RequisicaoResumo req) {
		BigDecimal pesoTotal = calcularPesoTotal(req.getItens());

		if ("MOTOBOY".equals(req.getModalidadeEntrega())) {
			if (pesoTotal.compareTo(new BigDecimal(LIMITE_MOTOBOY_KG)) > 0) {
				return "MODALIDADE_INDISPONIVEL";
			}
		}

		BigDecimal totalPedido = calcularTotalPedido(req);

		if ("BOLETO".equals(req.getFormaPagamento())) {
			if (totalPedido.compareTo(LIMITE_BOLETO) > 0) {
				return "FORMA_PAGAMENTO_INDISPONIVEL";
			}
		}

		return null;
	}

	public RespostaResumo calcularResumo(RequisicaoResumo req) {
		BigDecimal subtotalProdutos = calcularSubtotalProdutos(req.getItens());
		BigDecimal descontoCupom = calcularDescontoCupom(req.getItens(), req.getCupom(), subtotalProdutos);
		BigDecimal frete = calcularFrete(req.getItens(), req.getModalidadeEntrega());
		Integer prazoEntregaDias = getPrazoEntrega(req.getModalidadeEntrega());

		BigDecimal totalPedidoSemAjuste = subtotalProdutos.subtract(descontoCupom).add(frete);
		totalPedidoSemAjuste = arredondar(totalPedidoSemAjuste);

		int parcelas = req.getParcelas() != null ? req.getParcelas() : 1;
		BigDecimal ajustePagamento = calcularAjustePagamento(req.getFormaPagamento(), totalPedidoSemAjuste, parcelas);
		BigDecimal totalFinal = totalPedidoSemAjuste.add(ajustePagamento);
		totalFinal = arredondar(totalFinal);

		BigDecimal valorParcela = calcularValorParcela(req.getFormaPagamento(), totalPedidoSemAjuste, totalFinal, parcelas);

		return new RespostaResumo(subtotalProdutos, descontoCupom, frete, prazoEntregaDias, ajustePagamento,
				totalFinal, parcelas, valorParcela);
	}

	private BigDecimal calcularSubtotalProdutos(java.util.List<ItemCarrinho> itens) {
		BigDecimal subtotal = BigDecimal.ZERO;
		for (ItemCarrinho item : itens) {
			BigDecimal precoItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
			subtotal = subtotal.add(precoItem);
		}
		return arredondar(subtotal);
	}

	private BigDecimal calcularDescontoCupom(java.util.List<ItemCarrinho> itens, String cupom,
			BigDecimal subtotalProdutos) {
		if (cupom == null) {
			return arredondar(BigDecimal.ZERO);
		}

		if ("BEMVINDO10".equals(cupom)) {
			BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
			return arredondar(desconto);
		}

		if ("MENOS50".equals(cupom)) {
			return arredondar(new BigDecimal("50.00"));
		}

		if ("FRETEGRATIS".equals(cupom)) {
			return BigDecimal.ZERO;
		}

		if ("LEVE3PAGUE2".equals(cupom)) {
			return calcularDescontoLeve3Pague2(itens);
		}

		return BigDecimal.ZERO;
	}

	private BigDecimal calcularDescontoLeve3Pague2(java.util.List<ItemCarrinho> itens) {
		BigDecimal desconto = arredondar(BigDecimal.ZERO);
		for (ItemCarrinho item : itens) {
			int unidadesGratis = item.getQuantidade() / 3;
			BigDecimal precoItemGratis = item.getPrecoUnitario().multiply(new BigDecimal(unidadesGratis));
			desconto = desconto.add(precoItemGratis);
		}
		return arredondar(desconto);
	}

	private BigDecimal calcularFrete(java.util.List<ItemCarrinho> itens, String modalidadeEntrega) {
		EntregaConfig config = MODALIDADES_ENTREGA.get(modalidadeEntrega);
		if (config == null) {
			return BigDecimal.ZERO;
		}

		if ("RETIRADA_LOJA".equals(modalidadeEntrega)) {
			return arredondar(BigDecimal.ZERO);
		}

		if ("FRETEGRATIS".equals(modalidadeEntrega)) {
			return arredondar(BigDecimal.ZERO);
		}

		BigDecimal pesoTotal = calcularPesoTotal(itens);
		BigDecimal freteVariavel = pesoTotal.multiply(config.custoKg);
		BigDecimal freteTotal = config.custoBase.add(freteVariavel);

		return arredondar(freteTotal);
	}

	private BigDecimal calcularPesoTotal(java.util.List<ItemCarrinho> itens) {
		BigDecimal pesoTotal = BigDecimal.ZERO;
		for (ItemCarrinho item : itens) {
			BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
			pesoTotal = pesoTotal.add(pesoItem);
		}
		return pesoTotal;
	}

	private Integer getPrazoEntrega(String modalidadeEntrega) {
		EntregaConfig config = MODALIDADES_ENTREGA.get(modalidadeEntrega);
		return config != null ? config.prazo : 0;
	}

	private BigDecimal calcularTotalPedido(RequisicaoResumo req) {
		BigDecimal subtotalProdutos = calcularSubtotalProdutos(req.getItens());
		BigDecimal descontoCupom = calcularDescontoCupom(req.getItens(), req.getCupom(), subtotalProdutos);
		BigDecimal frete = calcularFrete(req.getItens(), req.getModalidadeEntrega());
		BigDecimal total = subtotalProdutos.subtract(descontoCupom).add(frete);
		return arredondar(total);
	}

	private BigDecimal calcularAjustePagamento(String formaPagamento, BigDecimal totalPedido, int parcelas) {
		if ("PIX".equals(formaPagamento)) {
			BigDecimal desconto = totalPedido.multiply(PIX_DESCONTO);
			return arredondar(desconto).negate();
		}

		if ("BOLETO".equals(formaPagamento)) {
			return arredondar(BOLETO_TARIFA);
		}

		if ("CARTAO".equals(formaPagamento)) {
			if (parcelas <= 3) {
				return arredondar(BigDecimal.ZERO);
			}
			BigDecimal jurosTotal = calcularJurosCartao(totalPedido, parcelas);
			return arredondar(jurosTotal);
		}

		return arredondar(BigDecimal.ZERO);
	}

	private BigDecimal calcularJurosCartao(BigDecimal totalPedido, int parcelas) {
		BigDecimal taxa = TAXA_JUROS_CARTAO;
		BigDecimal expoente = taxa.add(BigDecimal.ONE).pow(parcelas);
		BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(expoente, 20, ROUNDING_MODE));
		BigDecimal parcela = totalPedido.multiply(taxa).divide(divisor, 20, ROUNDING_MODE);
		parcela = arredondar(parcela);
		BigDecimal jurosTotal = parcela.multiply(new BigDecimal(parcelas)).subtract(totalPedido);
		return jurosTotal;
	}

	private BigDecimal calcularValorParcela(String formaPagamento, BigDecimal totalPedido, BigDecimal totalFinal, int parcelas) {
		if (parcelas <= 1) {
			return arredondar(totalFinal);
		}

		if ("PIX".equals(formaPagamento) || "BOLETO".equals(formaPagamento)) {
			return arredondar(totalFinal);
		}

		if ("CARTAO".equals(formaPagamento)) {
			if (parcelas <= 3) {
				BigDecimal valor = totalFinal.divide(new BigDecimal(parcelas), 20, ROUNDING_MODE);
				return arredondar(valor);
			}

			BigDecimal taxa = TAXA_JUROS_CARTAO;
			BigDecimal expoente = taxa.add(BigDecimal.ONE).pow(parcelas);
			BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(expoente, 20, ROUNDING_MODE));
			BigDecimal parcela = totalPedido.multiply(taxa).divide(divisor, 20, ROUNDING_MODE);
			return arredondar(parcela);
		}

		return arredondar(totalFinal);
	}

	private BigDecimal arredondar(BigDecimal valor) {
		if (valor == null) {
			return BigDecimal.ZERO;
		}
		return valor.setScale(2, ROUNDING_MODE);
	}

	private static class EntregaConfig {
		BigDecimal custoBase;
		BigDecimal custoKg;
		int prazo;

		EntregaConfig(BigDecimal custoBase, BigDecimal custoKg, int prazo) {
			this.custoBase = custoBase;
			this.custoKg = custoKg;
			this.prazo = prazo;
		}
	}

	private static class CupomConfig {
		String codigo;

		CupomConfig(String codigo) {
			this.codigo = codigo;
		}
	}
}
