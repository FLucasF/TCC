package com.loja.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import com.loja.model.Item;
import com.loja.model.CheckoutRequest;
import com.loja.model.CheckoutResponse;
import com.loja.coupon.Coupon;
import com.loja.coupon.CouponRegistry;
import com.loja.coupon.FretegratisCalculator;
import com.loja.delivery.DeliveryMode;
import com.loja.delivery.DeliveryModeRegistry;
import com.loja.payment.PaymentForm;
import com.loja.payment.PaymentFormRegistry;
import com.loja.util.MoneyRounder;

@Service
public class CheckoutService {

    public CheckoutResponse calculateCheckout(CheckoutRequest request) throws Exception {
        // Validation
        String validationError = validateRequest(request);
        if (validationError != null) {
            throw new ValidationException(validationError);
        }

        List<Item> items = request.getItens();
        String deliveryCode = request.getModalidadeEntrega();
        String couponCode = request.getCupom();
        String paymentCode = request.getFormaPagamento();
        int installments = request.getParcelas();

        // Calculate product subtotal
        BigDecimal subtotal = calculateSubtotal(items);

        // Get delivery mode and validate
        DeliveryMode deliveryMode = DeliveryModeRegistry.getMode(deliveryCode);
        if (deliveryMode == null) {
            throw new ValidationException("MODALIDADE_INVALIDA");
        }

        double totalWeightKg = calculateTotalWeight(items);
        if (!deliveryMode.isAvailable(totalWeightKg)) {
            throw new ValidationException("MODALIDADE_INDISPONIVEL");
        }

        // Calculate coupon discount
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isEmpty()) {
            if (FretegratisCalculator.isFretegratis(couponCode)) {
                // FRETEGRATIS will be applied after calculating shipping
            } else {
                Coupon coupon = CouponRegistry.getCoupon(couponCode);
                if (coupon == null) {
                    throw new ValidationException("CUPOM_INVALIDO");
                }
                if (!coupon.isApplicable(subtotal)) {
                    throw new ValidationException("CUPOM_NAO_APLICAVEL");
                }
                couponDiscount = coupon.calculateDiscount(subtotal, items);
            }
        }

        // Calculate shipping
        BigDecimal shippingCost = deliveryMode.calculateShipping(totalWeightKg);

        // Apply FRETEGRATIS if applicable
        if (couponCode != null && FretegratisCalculator.isFretegratis(couponCode)) {
            couponDiscount = shippingCost;
        }

        // Calculate order total (subtotal - coupon + shipping)
        BigDecimal orderTotal = subtotal
            .subtract(couponDiscount)
            .add(shippingCost);
        orderTotal = MoneyRounder.round(orderTotal);

        // Get payment form and validate
        PaymentForm paymentForm = PaymentFormRegistry.getForm(paymentCode);
        if (paymentForm == null) {
            throw new ValidationException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!paymentForm.isAvailable(orderTotal, installments)) {
            if (installments < 1 || installments > paymentForm.getMaxInstallments()) {
                throw new ValidationException("PARCELAMENTO_INVALIDO");
            } else {
                throw new ValidationException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }

        // Calculate payment adjustment and final total
        BigDecimal adjustment = paymentForm.calculateAdjustment(orderTotal, installments);
        BigDecimal finalTotal = MoneyRounder.round(orderTotal.add(adjustment));

        // Calculate installment value
        BigDecimal installmentValue;
        if (installments == 1) {
            installmentValue = finalTotal;
        } else {
            installmentValue = MoneyRounder.round(finalTotal.divide(new BigDecimal(installments), 10, java.math.RoundingMode.HALF_EVEN));
        }

        return new CheckoutResponse(
            subtotal,
            couponDiscount,
            shippingCost,
            deliveryMode.getDeliveryDays(),
            adjustment,
            finalTotal,
            installments,
            installmentValue
        );
    }

    private BigDecimal calculateSubtotal(List<Item> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : items) {
            BigDecimal itemPrice = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
            BigDecimal itemSubtotal = itemPrice.multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(itemSubtotal);
        }
        return MoneyRounder.round(subtotal);
    }

    private double calculateTotalWeight(List<Item> items) {
        double totalWeight = 0;
        for (Item item : items) {
            totalWeight += item.getPesoKg() * item.getQuantidade();
        }
        return totalWeight;
    }

    private String validateRequest(CheckoutRequest request) {
        // Check items list
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (Item item : request.getItens()) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario() <= 0 ||
                item.getQuantidade() == null || item.getQuantidade() <= 0 ||
                item.getPesoKg() == null || item.getPesoKg() <= 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        // Check delivery mode
        if (request.getModalidadeEntrega() == null || request.getModalidadeEntrega().isEmpty()) {
            return "MODALIDADE_INVALIDA";
        }

        // Check payment form
        if (request.getFormaPagamento() == null || request.getFormaPagamento().isEmpty()) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        return null;
    }
}
