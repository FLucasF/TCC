package com.loja.checkout.service;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomRegistry;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
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

    public ResumoResponse calcularResumo(ResumoRequest requisicao) {
        List<ItemPedido> itens = validarEConverterItens(requisicao.itens());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(
                itens.stream()
                        .map(ItemPedido::subtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        ModalidadeEntrega modalidade = modalidadeEntregaRegistry.buscar(requisicao.modalidadeEntrega());
        BigDecimal pesoTotal = itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        modalidade.validarDisponibilidade(pesoTotal);
        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        int prazoEntregaDias = modalidade.prazoEntregaDias();

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (requisicao.cupom() != null && !requisicao.cupom().isBlank()) {
            Cupom cupom = cupomRegistry.buscar(requisicao.cupom());
            cupom.validarAplicavel(itens, subtotalProdutos, frete);
            descontoCupom = cupom.calcularDesconto(itens, subtotalProdutos, frete);
        }

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(requisicao.formaPagamento());
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        formaPagamento.validarParcelas(parcelas);
        formaPagamento.validarDisponibilidade(totalPedido);

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(
                resultadoPagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazoEntregaDias,
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela());
    }

    private List<ItemPedido> validarEConverterItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itensRequest.stream()
                .map(this::validarEConverterItem)
                .toList();
    }

    private ItemPedido validarEConverterItem(ItemRequest itemRequest) {
        if (itemRequest.precoUnitario() == null || itemRequest.precoUnitario().signum() <= 0
                || itemRequest.quantidade() == null || itemRequest.quantidade() <= 0
                || itemRequest.pesoKg() == null || itemRequest.pesoKg().signum() <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(
                itemRequest.nome(),
                itemRequest.precoUnitario(),
                itemRequest.quantidade(),
                itemRequest.pesoKg());
    }
}
