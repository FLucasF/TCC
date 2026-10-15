package com.loja.checkout;

import com.loja.checkout.coupon.CouponService;
import com.loja.checkout.coupon.CupomStrategy;
import com.loja.checkout.delivery.DeliveryService;
import com.loja.checkout.delivery.ModalidadeEntregaStrategy;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import com.loja.checkout.payment.FormaPagamentoStrategy;
import com.loja.checkout.payment.PaymentService;
import com.loja.checkout.payment.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final DeliveryService deliveryService;
    private final CouponService couponService;
    private final PaymentService paymentService;

    public CheckoutService(DeliveryService deliveryService, CouponService couponService, PaymentService paymentService) {
        this.deliveryService = deliveryService;
        this.couponService = couponService;
        this.paymentService = paymentService;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        if (request == null) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }
        PedidoContext pedido = montarPedido(request.itens());

        BigDecimal subtotalProdutos = MoneyUtils.round(pedido.subtotalProdutos());

        ModalidadeEntregaStrategy modalidade = deliveryService.resolver(request.modalidadeEntrega(), pedido);
        BigDecimal frete = MoneyUtils.round(modalidade.calcularFrete(pedido));

        BigDecimal descontoCupom = MoneyUtils.round(BigDecimal.ZERO);
        if (request.cupom() != null) {
            CupomStrategy cupom = couponService.resolver(request.cupom(), pedido, subtotalProdutos);
            descontoCupom = MoneyUtils.round(cupom.calcularDesconto(pedido, subtotalProdutos, frete));
        }

        BigDecimal totalPedido = MoneyUtils.round(subtotalProdutos.subtract(descontoCupom).add(frete));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        FormaPagamentoStrategy formaPagamento = paymentService.resolver(request.formaPagamento(), parcelas, totalPedido);
        ResultadoPagamento resultadoPagamento = formaPagamento.aplicar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela()
        );
    }

    private PedidoContext montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal pesoTotal = BigDecimal.ZERO;
        List<PedidoContext.ItemPedido> itensPedido = new java.util.ArrayList<>();

        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
            }

            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            subtotal = subtotal.add(item.precoUnitario().multiply(quantidade));
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(quantidade));
            itensPedido.add(new PedidoContext.ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }

        return new PedidoContext(itensPedido, subtotal, pesoTotal);
    }
}
