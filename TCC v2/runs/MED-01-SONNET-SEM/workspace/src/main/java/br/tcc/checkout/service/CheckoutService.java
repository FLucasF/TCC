package br.tcc.checkout.service;

import br.tcc.checkout.api.dto.CheckoutRequest;
import br.tcc.checkout.api.dto.ItemPedidoRequest;
import br.tcc.checkout.api.dto.ResumoCompraResponse;
import br.tcc.checkout.cupom.Cupom;
import br.tcc.checkout.cupom.CupomRegistry;
import br.tcc.checkout.dominio.DadosPedido;
import br.tcc.checkout.entrega.EntregaRegistry;
import br.tcc.checkout.entrega.OpcaoEntrega;
import br.tcc.checkout.exception.CheckoutErrorCode;
import br.tcc.checkout.exception.CheckoutException;
import br.tcc.checkout.pagamento.FormaPagamento;
import br.tcc.checkout.pagamento.PagamentoService;
import br.tcc.checkout.pagamento.ResultadoPagamento;
import br.tcc.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

	private final EntregaRegistry entregaRegistry;
	private final CupomRegistry cupomRegistry;
	private final PagamentoService pagamentoService;

	public CheckoutService(EntregaRegistry entregaRegistry, CupomRegistry cupomRegistry,
			PagamentoService pagamentoService) {
		this.entregaRegistry = entregaRegistry;
		this.cupomRegistry = cupomRegistry;
		this.pagamentoService = pagamentoService;
	}

	public ResumoCompraResponse calcularResumo(CheckoutRequest request) {
		validarItens(request.itens());
		DadosPedido dadosPedido = construirDadosPedido(request.itens());

		OpcaoEntrega opcaoEntrega = entregaRegistry.buscar(request.modalidadeEntrega());
		if (!opcaoEntrega.isDisponivel(dadosPedido)) {
			throw new CheckoutException(CheckoutErrorCode.MODALIDADE_INDISPONIVEL);
		}
		BigDecimal frete = Dinheiro.arredondar(opcaoEntrega.calcularFrete(dadosPedido));

		BigDecimal descontoCupom = new BigDecimal("0.00");
		if (request.cupom() != null) {
			Cupom cupom = cupomRegistry.buscar(request.cupom());
			if (!cupom.isAplicavel(dadosPedido)) {
				throw new CheckoutException(CheckoutErrorCode.CUPOM_NAO_APLICAVEL);
			}
			descontoCupom = Dinheiro.arredondar(cupom.calcularDesconto(dadosPedido, frete));
		}

		BigDecimal totalPedido = Dinheiro
				.arredondar(dadosPedido.subtotalProdutos().subtract(descontoCupom).add(frete));

		FormaPagamento formaPagamento = FormaPagamento.fromCodigo(request.formaPagamento());
		int parcelas = request.parcelas() == null ? 1 : request.parcelas();
		pagamentoService.validarParcelamento(formaPagamento, parcelas);
		pagamentoService.validarDisponibilidade(formaPagamento, totalPedido);

		ResultadoPagamento resultado = pagamentoService.calcular(formaPagamento, totalPedido, parcelas);

		return new ResumoCompraResponse(
				dadosPedido.subtotalProdutos(),
				descontoCupom,
				frete,
				opcaoEntrega.getPrazoDias(),
				resultado.ajuste(),
				resultado.totalFinal(),
				resultado.parcelas(),
				resultado.valorParcela());
	}

	private void validarItens(List<ItemPedidoRequest> itens) {
		if (itens == null || itens.isEmpty()) {
			throw new CheckoutException(CheckoutErrorCode.PEDIDO_INVALIDO);
		}
		for (ItemPedidoRequest item : itens) {
			if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
					|| item.quantidade() == null || item.quantidade() <= 0
					|| item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
				throw new CheckoutException(CheckoutErrorCode.PEDIDO_INVALIDO);
			}
		}
	}

	private DadosPedido construirDadosPedido(List<ItemPedidoRequest> itens) {
		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal peso = BigDecimal.ZERO;
		for (ItemPedidoRequest item : itens) {
			BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
			subtotal = subtotal.add(item.precoUnitario().multiply(quantidade));
			peso = peso.add(item.pesoKg().multiply(quantidade));
		}
		return new DadosPedido(itens, Dinheiro.arredondar(subtotal), peso);
	}
}
