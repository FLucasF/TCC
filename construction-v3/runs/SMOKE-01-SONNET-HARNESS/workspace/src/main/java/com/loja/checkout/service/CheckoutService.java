package com.loja.checkout.service;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.Money;
import com.loja.checkout.domain.PedidoContext;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public CheckoutService(ModalidadeEntregaRegistry modalidadeEntregaRegistry,
                            CupomRegistry cupomRegistry,
                            FormaPagamentoRegistry formaPagamentoRegistry) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemPedido> itens = validarItens(request.itens());

        ModalidadeEntrega modalidade = modalidadeEntregaRegistry.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        BigDecimal pesoTotal = itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = Money.round(itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal frete = Money.round(modalidade.calcularFrete(pesoTotal));

        Cupom cupom = null;
        if (request.cupom() != null) {
            cupom = cupomRegistry.buscar(request.cupom())
                    .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
        }

        PedidoContext contexto = new PedidoContext(itens, subtotalProdutos, frete);

        if (cupom != null && !cupom.aplicavel(contexto)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO
                : Money.round(cupom.calcularDesconto(contexto));

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(request.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();

        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete);

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultado.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                ajustePagamento,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela()
        );
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        return itensRequest.stream()
                .map(this::validarItem)
                .toList();
    }

    private ItemPedido validarItem(ItemRequest item) {
        if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }
}
