package br.tcc.checkout.aplicacao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.tcc.checkout.api.ItemRequest;
import br.tcc.checkout.api.ResumoRequest;
import br.tcc.checkout.api.ResumoResponse;
import br.tcc.checkout.cupom.CatalogoCupons;
import br.tcc.checkout.cupom.ContextoCupom;
import br.tcc.checkout.cupom.Cupom;
import br.tcc.checkout.dominio.CheckoutException;
import br.tcc.checkout.dominio.Dinheiro;
import br.tcc.checkout.dominio.ErroCheckout;
import br.tcc.checkout.dominio.ItemPedido;
import br.tcc.checkout.dominio.Pedido;
import br.tcc.checkout.entrega.CatalogoEntregas;
import br.tcc.checkout.entrega.ModalidadeEntrega;
import br.tcc.checkout.pagamento.CatalogoPagamentos;
import br.tcc.checkout.pagamento.FormaPagamento;
import br.tcc.checkout.pagamento.ResultadoPagamento;

/**
 * Monta o resumo da compra: produtos, cupom, frete, total do pedido e o ajuste
 * da forma de pagamento, nessa ordem.
 */
@Service
public class CalculadoraResumo {

	private static final int PARCELAS_PADRAO = 1;

	private final CatalogoEntregas entregas;
	private final CatalogoCupons cupons;
	private final CatalogoPagamentos pagamentos;

	public CalculadoraResumo(CatalogoEntregas entregas, CatalogoCupons cupons, CatalogoPagamentos pagamentos) {
		this.entregas = entregas;
		this.cupons = cupons;
		this.pagamentos = pagamentos;
	}

	public ResumoResponse calcular(ResumoRequest request) {
		if (request == null) {
			throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
		}

		Pedido pedido = lerPedido(request.itens());

		ModalidadeEntrega entrega = entregas.buscar(request.modalidadeEntrega())
				.orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));
		if (!entrega.atende(pedido)) {
			throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
		}

		BigDecimal subtotalProdutos = pedido.subtotalProdutos();
		BigDecimal frete = entrega.calcularFrete(pedido);
		BigDecimal descontoCupom = calcularDesconto(request.cupom(), new ContextoCupom(pedido, frete));

		BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

		FormaPagamento pagamento = pagamentos.buscar(request.formaPagamento())
				.orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
		int parcelas = request.parcelas() == null ? PARCELAS_PADRAO : request.parcelas();
		if (!pagamento.aceitaParcelas(parcelas)) {
			throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
		}
		if (!pagamento.atende(totalPedido)) {
			throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
		}

		ResultadoPagamento resultado = pagamento.aplicar(totalPedido, parcelas);
		BigDecimal ajuste = Dinheiro.arredondar(resultado.totalFinal().subtract(totalPedido));

		return new ResumoResponse(
				subtotalProdutos,
				descontoCupom,
				frete,
				entrega.prazoDias(),
				ajuste,
				resultado.totalFinal(),
				resultado.parcelas(),
				resultado.valorParcela());
	}

	private Pedido lerPedido(List<ItemRequest> itens) {
		if (itens == null || itens.isEmpty()) {
			throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
		}
		List<ItemPedido> convertidos = new ArrayList<>(itens.size());
		for (ItemRequest item : itens) {
			if (item == null
					|| !positivo(item.precoUnitario())
					|| item.quantidade() == null || item.quantidade() <= 0
					|| !positivo(item.pesoKg())) {
				throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
			}
			convertidos.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
		}
		return new Pedido(convertidos);
	}

	private boolean positivo(BigDecimal valor) {
		return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
	}

	private BigDecimal calcularDesconto(String codigoCupom, ContextoCupom contexto) {
		if (codigoCupom == null || codigoCupom.isBlank()) {
			return Dinheiro.ZERO;
		}
		Cupom cupom = cupons.buscar(codigoCupom)
				.orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
		if (!cupom.aplicavel(contexto)) {
			throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
		}
		return cupom.calcularDesconto(contexto);
	}
}
