package com.loja.checkout.service;

import com.loja.checkout.coupon.CouponStrategy;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.payment.PaymentResult;
import com.loja.checkout.payment.PaymentStrategy;
import com.loja.checkout.shipping.ShippingStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ShippingStrategy> shippingStrategies;
    private final Map<String, CouponStrategy> couponStrategies;
    private final Map<String, PaymentStrategy> paymentStrategies;

    public CheckoutService(List<ShippingStrategy> shippingList,
                           List<CouponStrategy> couponList,
                           List<PaymentStrategy> paymentList) {
        this.shippingStrategies = shippingList.stream()
                .collect(Collectors.toMap(ShippingStrategy::getCode, Function.identity()));
        this.couponStrategies = couponList.stream()
                .collect(Collectors.toMap(CouponStrategy::getCode, Function.identity()));
        this.paymentStrategies = paymentList.stream()
                .collect(Collectors.toMap(PaymentStrategy::getCode, Function.identity()));
    }

    public CheckoutResponse calculate(CheckoutRequest request) {
        List<ItemRequest> items = validateItems(request.itens());

        BigDecimal subtotal = calculateSubtotal(items);
        BigDecimal totalWeight = calculateWeight(items);

        NivelClube nivel = NivelClube.fromString(request.nivelClube());
        if (nivel == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        Regiao regiao = Regiao.fromString(request.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        ShippingStrategy shipping = shippingStrategies.get(request.modalidadeEntrega());
        if (shipping == null) throw new CheckoutException("MODALIDADE_INVALIDA");
        if (!shipping.isAvailable(totalWeight)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        CouponStrategy coupon = resolveCoupon(request.cupom(), items, subtotal);

        String formaPagamento = request.formaPagamento();
        PaymentStrategy payment = paymentStrategies.get(formaPagamento);
        if (payment == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!payment.isInstallmentValid(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = shipping.calculateCost(totalWeight);
        if (nivel.hasFreeShipping()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = coupon != null
                ? coupon.calculateDiscount(items, subtotal, frete)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = subtotal.multiply(regiao.getInsuranceRate()).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!payment.isAvailable(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        PaymentResult paymentResult = payment.calculate(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = subtotal.multiply(nivel.getCreditRate()).setScale(2, RoundingMode.HALF_EVEN);

        boolean brinde = nivel.isGiftEligible() && subtotal.compareTo(new BigDecimal("500")) > 0;

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                shipping.getDeliveryDays(),
                seguro,
                paymentResult.ajuste(),
                paymentResult.totalFinal(),
                parcelas,
                paymentResult.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<ItemRequest> validateItems(List<ItemRequest> items) {
        if (items == null || items.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : items) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.quantidade() == null || item.quantidade() <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return items;
    }

    private BigDecimal calculateSubtotal(List<ItemRequest> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : items) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateWeight(List<ItemRequest> items) {
        BigDecimal weight = BigDecimal.ZERO;
        for (ItemRequest item : items) {
            weight = weight.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return weight;
    }

    private CouponStrategy resolveCoupon(String cupom, List<ItemRequest> items, BigDecimal subtotal) {
        if (cupom == null || cupom.isBlank()) return null;
        CouponStrategy strategy = couponStrategies.get(cupom);
        if (strategy == null) throw new CheckoutException("CUPOM_INVALIDO");
        if (!strategy.isApplicable(items, subtotal)) throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        return strategy;
    }
}
