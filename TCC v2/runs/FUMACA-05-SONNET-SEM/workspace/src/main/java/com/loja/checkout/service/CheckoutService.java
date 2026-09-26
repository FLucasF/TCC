package com.loja.checkout.service;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomResolver;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.PedidoContext;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaResolver;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import com.loja.checkout.pagamento.CalculadoraPagamento;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Dinheiro;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutService {

    private final ModalidadeEntregaResolver modalidadeEntregaResolver;
    private final CupomResolver cupomResolver;
    private final CalculadoraPagamento calculadoraPagamento;

    public CheckoutService(ModalidadeEntregaResolver modalidadeEntregaResolver,
                            CupomResolver cupomResolver,
                            CalculadoraPagamento calculadoraPagamento) {
        this.modalidadeEntregaResolver = modalidadeEntregaResolver;
        this.cupomResolver = cupomResolver;
        this.calculadoraPagamento = calculadoraPagamento;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        PedidoContext pedido = construirPedido(request.itens());

        ModalidadeEntrega modalidade = modalidadeEntregaResolver.resolver(request.modalidadeEntrega());
        if (!modalidade.disponivel(pedido)) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Dinheiro.arredondar(modalidade.calcularFrete(pedido));
        int prazoEntregaDias = modalidade.getPrazoDias();

        BigDecimal desconto = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null && !request.cupom().isBlank()) {
            Cupom cupom = cupomResolver.resolver(request.cupom());
            if (!cupom.aplicavel(pedido)) {
                throw new CheckoutException(ErroCodigo.CUPOM_NAO_APLICAVEL);
            }
            desconto = Dinheiro.arredondar(cupom.calcularDesconto(pedido, frete));
        }

        FormaPagamento formaPagamento = parseFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        calculadoraPagamento.validarParcelas(formaPagamento, parcelas);

        BigDecimal totalPedido = Dinheiro.arredondar(
                pedido.getSubtotalProdutos().subtract(desconto).add(frete));

        calculadoraPagamento.validarDisponibilidade(formaPagamento, totalPedido);

        ResultadoPagamento resultado = calculadoraPagamento.calcular(formaPagamento, parcelas, totalPedido);

        return new ResumoResponse(
                pedido.getSubtotalProdutos(),
                desconto,
                frete,
                prazoEntregaDias,
                resultado.ajuste(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela()
        );
    }

    private PedidoContext construirPedido(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (ItemRequest itemRequest : itensRequest) {
            if (itemRequest.precoUnitario() == null || itemRequest.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || itemRequest.quantidade() == null || itemRequest.quantidade() <= 0
                    || itemRequest.pesoKg() == null || itemRequest.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
            }

            ItemPedido item = new ItemPedido(
                    itemRequest.nome(), itemRequest.precoUnitario(), itemRequest.quantidade(), itemRequest.pesoKg());
            itens.add(item);
            subtotal = subtotal.add(item.totalItem());
            pesoTotal = pesoTotal.add(item.pesoTotalItem());
        }

        return new PedidoContext(itens, Dinheiro.arredondar(subtotal), pesoTotal);
    }

    private FormaPagamento parseFormaPagamento(String valor) {
        if (valor == null) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
        try {
            return FormaPagamento.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
    }
}
