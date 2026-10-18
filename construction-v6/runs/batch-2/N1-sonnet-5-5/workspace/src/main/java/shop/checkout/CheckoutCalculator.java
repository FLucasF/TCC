package shop.checkout;

import shop.ApiException;
import shop.Money;
import shop.catalog.Product;
import shop.coupon.Coupon;
import shop.coupon.CouponContext;
import shop.payment.PaymentCharge;
import shop.payment.PaymentOption;
import shop.persistence.Tx;
import shop.shipping.ShippingContext;
import shop.shipping.ShippingOption;
import shop.shipping.ShippingQuote;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** The only place that computes the purchase summary, used both by the preview and by order registration. */
public final class CheckoutCalculator {
    private static final Pattern ZIP = Pattern.compile("\\d{8}");
    private static final BigDecimal COUPON_CEILING = new BigDecimal("0.30");

    private final Map<String, ShippingOption> shippingOptions;
    private final Map<String, Coupon> coupons;
    private final Map<String, PaymentOption> paymentOptions;

    public CheckoutCalculator(Map<String, ShippingOption> shippingOptions, Map<String, Coupon> coupons,
                              Map<String, PaymentOption> paymentOptions) {
        this.shippingOptions = shippingOptions;
        this.coupons = coupons;
        this.paymentOptions = paymentOptions;
    }

    public Summary calculate(Tx tx, CheckoutRequest request) {
        validateShape(request);
        List<CartLine> lines = loadLines(tx, request);

        ClubTier tier = parse(ClubTier.class, request.clubTier(), "INVALID_CLUB_TIER");
        Region region = parse(Region.class, request.region(), "INVALID_REGION");
        if (request.zipCode() == null || !ZIP.matcher(request.zipCode()).matches()) {
            throw ApiException.unprocessable("INVALID_ZIP_CODE");
        }
        ShippingOption shipping = request.shippingMethod() == null ? null : shippingOptions.get(request.shippingMethod());
        if (shipping == null) {
            throw ApiException.unprocessable("INVALID_SHIPPING_METHOD");
        }

        BigDecimal subtotal = Money.ZERO;
        BigDecimal weightKg = BigDecimal.ZERO;
        for (CartLine line : lines) {
            subtotal = subtotal.add(line.total());
            weightKg = weightKg.add(line.weightKg());
        }
        subtotal = Money.round(subtotal);

        ShippingQuote quote = shipping.quote(new ShippingContext(request.zipCode(), region, weightKg));
        BigDecimal shippingCost = tier.waivesShipping() && shipping.acceptsClubFreeShipping()
                ? Money.ZERO : Money.round(quote.cost());
        BigDecimal insurance = region.insurance(subtotal);

        BigDecimal discount = Money.ZERO;
        if (request.coupon() != null) {
            Coupon coupon = coupons.get(request.coupon());
            if (coupon == null) {
                throw ApiException.unprocessable("INVALID_COUPON");
            }
            BigDecimal raw = coupon.discount(new CouponContext(lines, subtotal, shippingCost, shipping.hasShipping(),
                    () -> !tx.orders().hasConfirmedPurchase(request.cpf())));
            BigDecimal ceiling = Money.round(subtotal.multiply(COUPON_CEILING));
            discount = Money.round(raw.min(ceiling));
        }

        PaymentOption payment = request.paymentMethod() == null ? null : paymentOptions.get(request.paymentMethod());
        if (payment == null) {
            throw ApiException.unprocessable("INVALID_PAYMENT_METHOD");
        }
        if (!payment.acceptsInstallments(request.installments())) {
            throw ApiException.unprocessable("INVALID_INSTALLMENTS");
        }
        BigDecimal orderTotal = Money.round(subtotal.subtract(discount).add(shippingCost).add(insurance));
        if (!payment.availableFor(orderTotal)) {
            throw ApiException.unprocessable("PAYMENT_METHOD_UNAVAILABLE");
        }
        PaymentCharge charge = payment.charge(orderTotal, request.installments(), request.coupon() != null);

        return new Summary(subtotal, discount, shippingCost, quote.days(), insurance,
                charge.finalTotal().subtract(orderTotal), charge.finalTotal(), request.installments(),
                charge.installmentAmount(), tier.nextPurchaseCredit(subtotal), tier.gift(subtotal));
    }

    private static void validateShape(CheckoutRequest request) {
        boolean valid = request.cpf() != null && !request.cpf().isBlank()
                && request.email() != null && !request.email().isBlank()
                && request.items() != null && !request.items().isEmpty();
        if (valid) {
            Set<String> seen = new HashSet<>();
            for (CheckoutRequest.Item item : request.items()) {
                if (item.sku() == null || item.sku().isBlank() || item.quantity() == null || item.quantity() <= 0
                        || !seen.add(item.sku())) {
                    valid = false;
                    break;
                }
            }
        }
        if (!valid) {
            throw ApiException.unprocessable("INVALID_ORDER");
        }
    }

    private static List<CartLine> loadLines(Tx tx, CheckoutRequest request) {
        List<CartLine> lines = new ArrayList<>();
        for (CheckoutRequest.Item item : request.items()) {
            Product product = tx.products().find(item.sku())
                    .orElseThrow(() -> ApiException.unprocessable("UNKNOWN_PRODUCT"));
            lines.add(new CartLine(product, item.quantity()));
        }
        if (lines.stream().anyMatch(line -> line.product().discontinued())) {
            throw ApiException.unprocessable("PRODUCT_DISCONTINUED");
        }
        if (lines.stream().anyMatch(line -> line.product().stock() < line.quantity())) {
            throw ApiException.unprocessable("INSUFFICIENT_STOCK");
        }
        return lines;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String errorCode) {
        if (value != null) {
            for (E constant : type.getEnumConstants()) {
                if (constant.name().equals(value)) {
                    return constant;
                }
            }
        }
        throw ApiException.unprocessable(errorCode);
    }
}
