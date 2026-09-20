package br.tcc.checkout.api;

import br.tcc.checkout.dominio.CheckoutException;
import br.tcc.checkout.dominio.Cobranca;
import br.tcc.checkout.dominio.ContextoCupom;
import br.tcc.checkout.dominio.ContextoPagamento;
import br.tcc.checkout.dominio.Cupom;
import br.tcc.checkout.dominio.Dinheiro;
import br.tcc.checkout.dominio.Erros;
import br.tcc.checkout.dominio.FormaPagamento;
import br.tcc.checkout.dominio.ItemPedido;
import br.tcc.checkout.dominio.ModalidadeEntrega;
import br.tcc.checkout.dominio.Pedido;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * A sequência do checkout, igual para todo pedido: produtos, cupom, frete,
 * total e ajuste do pagamento. O que muda de caso para caso mora na
 * modalidade de entrega, no cupom e na forma de pagamento.
 */
@Service
public class ResumoCompraService {

	private static final int PARCELAS_PADRAO = 1;

	public ResumoResponse calcular(ResumoRequest requisicao) {
		Pedido pedido = pedidoDe(requisicao);

		ModalidadeEntrega modalidade = ModalidadeEntrega.porCodigo(requisicao.modalidadeEntrega())
				.orElseThrow(() -> new CheckoutException(Erros.MODALIDADE_INVALIDA));
		if (!modalidade.atende(pedido)) {
			throw new CheckoutException(Erros.MODALIDADE_INDISPONIVEL);
		}
		BigDecimal frete = Dinheiro.centavos(modalidade.frete(pedido));

		BigDecimal desconto = descontoDoCupom(requisicao, new ContextoCupom(pedido, modalidade, frete));

		BigDecimal subtotal = pedido.subtotalProdutos();
		BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(desconto).add(frete));

		FormaPagamento pagamento = FormaPagamento.porCodigo(requisicao.formaPagamento())
				.orElseThrow(() -> new CheckoutException(Erros.FORMA_PAGAMENTO_INVALIDA));
		int parcelas = requisicao.parcelas() == null ? PARCELAS_PADRAO : requisicao.parcelas();
		if (!pagamento.parcelamentoPermitido(parcelas)) {
			throw new CheckoutException(Erros.PARCELAMENTO_INVALIDO);
		}
		ContextoPagamento contexto = new ContextoPagamento(totalPedido, parcelas);
		if (!pagamento.atende(contexto)) {
			throw new CheckoutException(Erros.FORMA_PAGAMENTO_INDISPONIVEL);
		}
		Cobranca cobranca = pagamento.cobranca(contexto);

		return new ResumoResponse(
				subtotal,
				desconto,
				frete,
				modalidade.prazoDias(),
				Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
				cobranca.totalFinal(),
				parcelas,
				cobranca.valorParcela());
	}

	private Pedido pedidoDe(ResumoRequest requisicao) {
		if (requisicao == null || requisicao.itens() == null || requisicao.itens().isEmpty()) {
			throw new CheckoutException(Erros.PEDIDO_INVALIDO);
		}
		List<ItemPedido> itens = requisicao.itens().stream()
				.map(this::itemDe)
				.toList();
		return new Pedido(itens);
	}

	private ItemPedido itemDe(ResumoRequest.ItemRequest item) {
		if (item == null
				|| invalido(item.precoUnitario())
				|| item.quantidade() == null || item.quantidade() <= 0
				|| invalido(item.pesoKg())) {
			throw new CheckoutException(Erros.PEDIDO_INVALIDO);
		}
		return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
	}

	private boolean invalido(BigDecimal valor) {
		return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
	}

	private BigDecimal descontoDoCupom(ResumoRequest requisicao, ContextoCupom contexto) {
		String codigo = requisicao.cupom();
		if (codigo == null || codigo.isBlank()) {
			return Dinheiro.ZERO;
		}
		Cupom cupom = Cupom.porCodigo(codigo)
				.orElseThrow(() -> new CheckoutException(Erros.CUPOM_INVALIDO));
		if (!cupom.aplicavel(contexto)) {
			throw new CheckoutException(Erros.CUPOM_NAO_APLICAVEL);
		}
		return Dinheiro.centavos(cupom.desconto(contexto));
	}
}
