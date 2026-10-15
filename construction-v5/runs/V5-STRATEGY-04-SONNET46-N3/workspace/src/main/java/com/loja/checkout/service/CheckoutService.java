package com.loja.checkout.service;

import com.loja.checkout.domain.CartItem;
import com.loja.checkout.domain.CheckoutRequest;
import com.loja.checkout.domain.CheckoutResponse;
import com.loja.checkout.domain.ClubLevel;
import com.loja.checkout.domain.PaymentMethod;
import com.loja.checkout.domain.Region;
import com.loja.checkout.domain.ShippingOption;
import com.loja.checkout.domain.coupon.Coupon;
import com.loja.checkout.domain.coupon.CouponRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutResponse calculate(CheckoutRequest req) {
        // 1. Validate items
        validateItems(req.itens());
        BigDecimal subtotal = computeSubtotal(req.itens());
        BigDecimal totalWeight = computeTotalWeight(req.itens());

        // 2. Validate club level
        ClubLevel club = parseEnum(ClubLevel.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        // 3. Validate region
        Region region = parseEnum(Region.class, req.regiao(), "REGIAO_INVALIDA");

        // 4. Validate shipping option
        ShippingOption shipping = parseEnum(ShippingOption.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        // 5. Check shipping availability
        if (!shipping.isAvailable(totalWeight)) throw new CheckoutError("MODALIDADE_INDISPONIVEL");

        BigDecimal frete = club.freeShipping()
                ? BigDecimal.ZERO.setScale(2)
                : shipping.cost(totalWeight);

        // 6. Validate coupon (if provided)
        Coupon coupon = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            coupon = CouponRegistry.find(req.cupom())
                    .orElseThrow(() -> new CheckoutError("CUPOM_INVALIDO"));
            // 7. Check coupon applicability
            if (!coupon.isApplicable(subtotal, req.itens())) throw new CheckoutError("CUPOM_NAO_APLICAVEL");
        }

        BigDecimal descontoCupom = coupon != null
                ? coupon.discount(subtotal, req.itens(), frete)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = region.insuranceOn(subtotal);
        BigDecimal orderTotal = subtotal.subtract(descontoCupom).add(frete).add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        // 8. Validate payment method
        PaymentMethod payment = parseEnum(PaymentMethod.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;

        // 9. Validate parcelas
        if (!payment.validParcelas(parcelas)) throw new CheckoutError("PARCELAMENTO_INVALIDO");

        // 10. Check payment availability
        if (!payment.isAvailable(orderTotal)) throw new CheckoutError("FORMA_PAGAMENTO_INDISPONIVEL");

        BigDecimal totalFinal = payment.finalTotal(orderTotal, parcelas);
        BigDecimal valorParcela = payment.installmentValue(orderTotal, parcelas);
        BigDecimal ajuste = totalFinal.subtract(orderTotal);

        BigDecimal credito = club.cashbackOn(subtotal);
        boolean brinde = club.hasGiftAbove(subtotal);

        return new CheckoutResponse(
                subtotal, descontoCupom, frete, shipping.deliveryDays(),
                seguro, ajuste, totalFinal, parcelas, valorParcela, credito, brinde
        );
    }

    private void validateItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) throw new CheckoutError("PEDIDO_INVALIDO");
        for (CartItem item : items) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutError("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal computeSubtotal(List<CartItem> items) {
        BigDecimal sum = BigDecimal.ZERO;
        for (CartItem item : items) {
            sum = sum.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal computeTotalWeight(List<CartItem> items) {
        BigDecimal sum = BigDecimal.ZERO;
        for (CartItem item : items) {
            sum = sum.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return sum;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> cls, String value, String errorCode) {
        if (value == null || value.isBlank()) throw new CheckoutError(errorCode);
        try {
            return Enum.valueOf(cls, value);
        } catch (IllegalArgumentException e) {
            throw new CheckoutError(errorCode);
        }
    }
}
