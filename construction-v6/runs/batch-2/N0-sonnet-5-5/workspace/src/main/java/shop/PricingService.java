package shop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Único lugar onde o resumo é calculado: o resumo e o registro do pedido passam por aqui. */
class PricingService {
    private static final Pattern ZIP = Pattern.compile("\\d{8}");

    private final ShippingMethods shipping;
    private final Coupons coupons;
    private final PaymentMethods payments;

    PricingService(ShippingMethods shipping, Coupons coupons, PaymentMethods payments) {
        this.shipping = shipping;
        this.coupons = coupons;
        this.payments = payments;
    }

    Summary calculate(CheckoutRequest r, Tx tx) {
        List<CartLine> lines = loadCart(r, tx);

        ClubTier tier = ClubTier.parse(r.clubTier());
        if (tier == null) throw ApiException.unprocessable("INVALID_CLUB_TIER");
        Region region = Region.parse(r.region());
        if (region == null) throw ApiException.unprocessable("INVALID_REGION");
        if (r.zipCode() == null || !ZIP.matcher(r.zipCode()).matches()) {
            throw ApiException.unprocessable("INVALID_ZIP_CODE");
        }
        ShippingMethod method = shipping.get(r.shippingMethod());
        if (method == null) throw ApiException.unprocessable("INVALID_SHIPPING_METHOD");

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal weight = BigDecimal.ZERO;
        for (CartLine l : lines) {
            subtotal = subtotal.add(l.total());
            weight = weight.add(l.product().weightKg().multiply(BigDecimal.valueOf(l.quantity())));
        }
        subtotal = Money.round(subtotal);

        ShippingQuote quote = method.quote(new ShippingContext(r.zipCode(), weight, region));
        BigDecimal shippingCost = tier.waivesShipping() && method.waivableByClub() ? Money.ZERO : quote.cost();

        BigDecimal couponDiscount = Money.ZERO;
        if (r.couponPresent()) {
            Coupon coupon = coupons.get(r.coupon());
            if (coupon == null) throw ApiException.unprocessable("INVALID_COUPON");
            CouponContext ctx = new CouponContext(lines, subtotal, shippingCost, method,
                    () -> !tx.hasFulfilledOrder(r.cpf()));
            couponDiscount = coupons.discountFor(coupon, ctx);
        }

        BigDecimal insurance = region.insurance(subtotal);
        BigDecimal orderTotal = Money.round(subtotal.subtract(couponDiscount).add(shippingCost).add(insurance));

        PaymentMethod payment = payments.get(r.paymentMethod());
        if (payment == null) throw ApiException.unprocessable("INVALID_PAYMENT_METHOD");
        if (!payment.acceptsInstallments(r.installments())) throw ApiException.unprocessable("INVALID_INSTALLMENTS");
        if (!payment.availableFor(orderTotal)) throw ApiException.unprocessable("PAYMENT_METHOD_UNAVAILABLE");
        PaymentOutcome outcome = payment.settle(orderTotal, r.installments(), r.couponPresent());
        BigDecimal finalTotal = Money.round(outcome.finalTotal());

        return new Summary(subtotal, couponDiscount, shippingCost, quote.days(), insurance,
                finalTotal.subtract(orderTotal), finalTotal, r.installments(), outcome.installmentAmount(),
                tier.credit(subtotal), tier.gift(subtotal));
    }

    private List<CartLine> loadCart(CheckoutRequest r, Tx tx) {
        if (r.cpf() == null || r.cpf().isBlank() || r.email() == null || r.email().isBlank()
                || r.items().isEmpty()) {
            throw ApiException.unprocessable("INVALID_ORDER");
        }
        Set<String> seen = new HashSet<>();
        for (CheckoutRequest.RawItem i : r.items()) {
            if (i.sku() == null || i.sku().isBlank() || i.quantity() == null || i.quantity() <= 0
                    || !seen.add(i.sku())) {
                throw ApiException.unprocessable("INVALID_ORDER");
            }
        }
        Map<String, Product> products = new HashMap<>();
        for (CheckoutRequest.RawItem i : r.items()) {
            Product p = tx.findProduct(i.sku()).orElseThrow(() -> ApiException.unprocessable("UNKNOWN_PRODUCT"));
            products.put(i.sku(), p);
        }
        for (Product p : products.values()) {
            if (p.discontinued()) throw ApiException.unprocessable("PRODUCT_DISCONTINUED");
        }
        List<CartLine> lines = new ArrayList<>();
        for (CheckoutRequest.RawItem i : r.items()) {
            Product p = products.get(i.sku());
            if (p.stock() < i.quantity()) throw ApiException.unprocessable("INSUFFICIENT_STOCK");
            lines.add(new CartLine(p, i.quantity()));
        }
        return lines;
    }
}
