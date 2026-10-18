package shop.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import shop.domain.CartLine;
import shop.domain.ClubTier;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.domain.Money;
import shop.domain.Product;
import shop.domain.Region;
import shop.domain.Summary;
import shop.domain.coupon.Coupon;
import shop.domain.coupon.CouponContext;
import shop.domain.coupon.Coupons;
import shop.domain.payment.PaymentMethod;
import shop.domain.payment.PaymentMethods;
import shop.domain.shipping.ShippingOption;
import shop.domain.shipping.ShippingOptions;
import shop.domain.shipping.ShippingQuote;
import shop.domain.shipping.ShippingRequest;
import shop.persistence.Tx;

/** Único lugar onde o resumo é calculado: o resumo e o pedido registrado usam o mesmo código. */
public class CheckoutCalculator {
    private static final Pattern ZIP = Pattern.compile("\\d{8}");

    private final ShippingOptions shippingOptions;
    private final Coupons coupons;
    private final PaymentMethods paymentMethods;

    public CheckoutCalculator(ShippingOptions shippingOptions, Coupons coupons, PaymentMethods paymentMethods) {
        this.shippingOptions = shippingOptions;
        this.coupons = coupons;
        this.paymentMethods = paymentMethods;
    }

    /** Valida na ordem definida e devolve o primeiro problema encontrado. */
    public Summary calculate(CheckoutRequest req, Tx tx) {
        List<CartLine> lines = cartLines(req, tx);
        ClubTier tier = ClubTier.parse(req.clubTier());
        Region region = Region.parse(req.region());
        if (req.zipCode() == null || !ZIP.matcher(req.zipCode()).matches()) {
            throw new DomainException(ErrorCode.INVALID_ZIP_CODE);
        }
        ShippingOption shipping = shippingOptions.find(req.shippingMethod())
                .orElseThrow(() -> new DomainException(ErrorCode.INVALID_SHIPPING_METHOD));

        BigDecimal subtotal = lines.stream().map(CartLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightKg = lines.stream().map(CartLine::weightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        ShippingQuote quote = shipping.quote(new ShippingRequest(req.zipCode(), weightKg, region));
        BigDecimal shippingCost = tier.waivesShipping(shipping) ? Money.ZERO : quote.cost();

        BigDecimal discount = Money.ZERO;
        if (req.coupon() != null) {
            Coupon coupon = coupons.find(req.coupon())
                    .orElseThrow(() -> new DomainException(ErrorCode.INVALID_COUPON));
            boolean firstPurchase = !tx.orders().hasPurchased(req.cpf());
            discount = Coupons.cap(coupon.discount(
                    new CouponContext(lines, subtotal, shippingCost, shipping, firstPurchase)), subtotal);
        }

        PaymentMethod payment = paymentMethods.find(req.paymentMethod())
                .orElseThrow(() -> new DomainException(ErrorCode.INVALID_PAYMENT_METHOD));
        if (!payment.acceptsInstallments(req.installments())) {
            throw new DomainException(ErrorCode.INVALID_INSTALLMENTS);
        }
        BigDecimal insurance = region.insurance(subtotal);
        BigDecimal orderTotal = subtotal.subtract(discount).add(shippingCost).add(insurance);
        if (!payment.availableFor(orderTotal)) {
            throw new DomainException(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
        }
        PaymentMethod.PaymentCharge charge = payment.charge(orderTotal, req.installments(), req.coupon() != null);

        return new Summary(subtotal, discount, shippingCost, quote.days(), insurance,
                charge.finalTotal().subtract(orderTotal), charge.finalTotal(), req.installments(),
                charge.installmentAmount(), tier.credit(subtotal), tier.gift(subtotal));
    }

    private List<CartLine> cartLines(CheckoutRequest req, Tx tx) {
        if (blank(req.cpf()) || blank(req.email()) || req.items() == null || req.items().isEmpty()) {
            throw new DomainException(ErrorCode.INVALID_ORDER);
        }
        Set<String> seen = new HashSet<>();
        for (CheckoutRequest.RequestItem item : req.items()) {
            if (item == null || blank(item.sku()) || item.quantity() == null || item.quantity() <= 0
                    || !seen.add(item.sku())) {
                throw new DomainException(ErrorCode.INVALID_ORDER);
            }
        }
        List<CartLine> lines = new ArrayList<>();
        for (CheckoutRequest.RequestItem item : req.items()) {
            Product product = tx.products().find(item.sku())
                    .orElseThrow(() -> new DomainException(ErrorCode.UNKNOWN_PRODUCT));
            lines.add(new CartLine(product, item.quantity()));
        }
        if (lines.stream().anyMatch(l -> l.product().discontinued())) {
            throw new DomainException(ErrorCode.PRODUCT_DISCONTINUED);
        }
        if (lines.stream().anyMatch(l -> l.product().stock() < l.quantity())) {
            throw new DomainException(ErrorCode.INSUFFICIENT_STOCK);
        }
        return lines;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
