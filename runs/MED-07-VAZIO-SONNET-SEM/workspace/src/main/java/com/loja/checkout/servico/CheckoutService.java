package com.loja.checkout.servico;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.PedidoContext;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.CalculoFrete;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.excecao.CheckoutException;
import com.loja.checkout.excecao.CodigoErro;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public CheckoutService(
            ModalidadeEntregaRegistry modalidadeEntregaRegistry,
            CupomRegistry cupomRegistry,
            FormaPagamentoRegistry formaPagamentoRegistry) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        PedidoContext pedido = construirPedido(request);

        ModalidadeEntrega modalidadeEntrega = modalidadeEntregaRegistry.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidadeEntrega.disponivel(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        CalculoFrete calculoFrete = modalidadeEntrega.calcular(pedido);
        BigDecimal frete = calculoFrete.valor();

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        String codigoCupom = request.cupom();
        if (codigoCupom != null && !codigoCupom.isBlank()) {
            Cupom cupom = cupomRegistry.buscar(codigoCupom)
                    .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
            if (!cupom.aplicavel(pedido)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.calcularDesconto(pedido, frete);
        }

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(request.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalPedido = Dinheiro.arredondar(
                pedido.subtotalProdutos().subtract(descontoCupom).add(frete));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                pedido.subtotalProdutos(),
                descontoCupom,
                frete,
                calculoFrete.prazoDias(),
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela());
    }

    private PedidoContext construirPedido(ResumoRequest request) {
        List<ItemRequest> itensRequest = request.itens();
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        BigDecimal subtotalProdutos = BigDecimal.ZERO;
        BigDecimal pesoTotalKg = BigDecimal.ZERO;
        List<ItemPedido> itens = new java.util.ArrayList<>();

        for (ItemRequest itemRequest : itensRequest) {
            if (itemRequest.precoUnitario() == null || itemRequest.precoUnitario().signum() <= 0
                    || itemRequest.quantidade() == null || itemRequest.quantidade() <= 0
                    || itemRequest.pesoKg() == null || itemRequest.pesoKg().signum() <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            ItemPedido item = new ItemPedido(
                    itemRequest.nome(), itemRequest.precoUnitario(), itemRequest.quantidade(), itemRequest.pesoKg());
            itens.add(item);
            subtotalProdutos = subtotalProdutos.add(item.subtotal());
            pesoTotalKg = pesoTotalKg.add(item.pesoTotal());
        }

        return new PedidoContext(itens, Dinheiro.arredondar(subtotalProdutos), pesoTotalKg);
    }
}
