package br.tcc.checkout.service;

import java.util.List;
import org.springframework.stereotype.Service;
import br.tcc.checkout.dto.CheckoutRequest;
import br.tcc.checkout.dto.CheckoutResponse;
import br.tcc.checkout.dto.ItemRequest;
import br.tcc.checkout.enums.Cupom;
import br.tcc.checkout.enums.FormaPagamento;
import br.tcc.checkout.enums.ModalidadeEntrega;
import br.tcc.checkout.exception.CheckoutException;
import br.tcc.checkout.util.MoneyUtil;

@Service
public class CheckoutService {

	public CheckoutResponse calcularResumo(CheckoutRequest request) {
		List<ItemRequest> itens = request.getItens();
		String modalidadeEntregaStr = request.getModalidadeEntrega();
		String cupomStr = request.getCupom();
		String formaPagamentoStr = request.getFormaPagamento();
		int parcelas = request.getParcelas();

		validarPedido(itens);

		ModalidadeEntrega modalidadeEntrega = ModalidadeEntrega.from(modalidadeEntregaStr);

		double pesoTotal = calcularPesoTotal(itens);

		if (!modalidadeEntrega.isDisponivel(pesoTotal)) {
			throw new CheckoutException("MODALIDADE_INDISPONIVEL");
		}

		Cupom cupom = null;
		if (cupomStr != null && !cupomStr.isEmpty()) {
			if (!Cupom.existe(cupomStr)) {
				throw new CheckoutException("CUPOM_INVALIDO");
			}
			cupom = Cupom.from(cupomStr);
		}

		FormaPagamento formaPagamento = FormaPagamento.from(formaPagamentoStr);

		if (!formaPagamento.isParcelaValida(parcelas)) {
			throw new CheckoutException("PARCELAMENTO_INVALIDO");
		}

		double subtotalProdutos = calcularSubtotalProdutos(itens);

		double frete = MoneyUtil.round(modalidadeEntrega.calcularFrete(pesoTotal));

		double desconto = 0.0;
		if (cupom != null) {
			if (!cupom.isAplicavel(subtotalProdutos, frete)) {
				throw new CheckoutException("CUPOM_NAO_APLICAVEL");
			}
			if (cupom == Cupom.LEVE3PAGUE2) {
				desconto = calcularDescontoLeve3Pague2(itens);
			} else {
				desconto = cupom.calcularDesconto(subtotalProdutos, frete);
			}
		}

		double totalSemAjuste = MoneyUtil.round(subtotalProdutos - desconto + frete);

		if (!formaPagamento.isDisponivel(totalSemAjuste)) {
			throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
		}

		double ajuste = formaPagamento.calcularAjuste(totalSemAjuste, parcelas);
		ajuste = MoneyUtil.round(ajuste);

		double totalFinal = MoneyUtil.round(totalSemAjuste + ajuste);

		double valorParcela = formaPagamento.calcularValorParcela(totalSemAjuste, parcelas);

		return new CheckoutResponse(subtotalProdutos, desconto, frete, modalidadeEntrega.getPrazo(),
				ajuste, totalFinal, parcelas, valorParcela);
	}

	private void validarPedido(List<ItemRequest> itens) {
		if (itens == null || itens.isEmpty()) {
			throw new CheckoutException("PEDIDO_INVALIDO");
		}

		for (ItemRequest item : itens) {
			if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0) {
				throw new CheckoutException("PEDIDO_INVALIDO");
			}
			if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
				throw new CheckoutException("PEDIDO_INVALIDO");
			}
			if (item.getPesoKg() == null || item.getPesoKg() <= 0) {
				throw new CheckoutException("PEDIDO_INVALIDO");
			}
		}
	}

	private double calcularSubtotalProdutos(List<ItemRequest> itens) {
		double subtotal = 0.0;
		for (ItemRequest item : itens) {
			subtotal += item.getPrecoUnitario() * item.getQuantidade();
		}
		return MoneyUtil.round(subtotal);
	}

	private double calcularPesoTotal(List<ItemRequest> itens) {
		double pesoTotal = 0.0;
		for (ItemRequest item : itens) {
			pesoTotal += item.getPesoKg() * item.getQuantidade();
		}
		return pesoTotal;
	}

	private double calcularDescontoLeve3Pague2(List<ItemRequest> itens) {
		double desconto = 0.0;
		for (ItemRequest item : itens) {
			int quantidade = item.getQuantidade();
			int unidadesGratis = quantidade / 3;
			double descontoItem = unidadesGratis * item.getPrecoUnitario();
			desconto += descontoItem;
		}
		return MoneyUtil.round(desconto);
	}
}
