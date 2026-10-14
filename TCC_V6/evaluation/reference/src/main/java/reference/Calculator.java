package reference;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import reference.Catalog.Product;
import reference.Purchase.Item;
import reference.Result.Refusal;
import reference.Result.Summary;

/**
 * The reference for the purchase summary ({@code POST /checkout/summary}).
 *
 * HOW TO READ. {@link #summarize} follows the prompt in order: first the 15 refusals, in the order of the
 * table "Quando dá erro"; then the calculation, step by step. A refusal stops the flow right away (the
 * {@link Refused} exception), so the main path reads straight down, like the prompt's text. Every amount
 * of money is rounded to cents at each step, half to even ({@link #round}).
 *
 * Wherever the prompt left a doubt, the choice made here is marked DECISION (and is already in the prompt).
 */
public final class Calculator {

    /** What the calculator needs besides the purchase: the current catalog, the history and the TransLog. */
    public record Context(Catalog catalog, Predicate<String> hasPaidOrder, TransLog transLog) {}

    static final List<String> CLUB_TIERS = List.of("BRONZE", "SILVER", "GOLD");
    static final List<String> SHIPPING_METHODS = List.of("ECONOMY", "EXPRESS", "STORE_PICKUP", "COURIER");
    static final List<String> COUPONS = List.of("WELCOME10", "MINUS50", "FREESHIPPING", "BUY3PAY2");
    static final List<String> PAYMENT_METHODS = List.of("PIX", "CREDIT_CARD", "BANK_SLIP");

    /** The insurance of each region: the rate and the limit (null = no limit). */
    record InsuranceRule(BigDecimal rate, BigDecimal limit) {}

    static final Map<String, InsuranceRule> INSURANCE = Map.of(
            "SOUTHEAST", new InsuranceRule(new BigDecimal("0.01"), null),
            "SOUTH", new InsuranceRule(new BigDecimal("0.01"), null),
            "MIDWEST", new InsuranceRule(new BigDecimal("0.015"), null),
            "NORTH", new InsuranceRule(new BigDecimal("0.025"), new BigDecimal("30.00")),
            "NORTHEAST", new InsuranceRule(new BigDecimal("0.02"), new BigDecimal("30.00")));

    /** The extra dispatch days, only for ECONOMY and EXPRESS. */
    static final Map<String, Integer> EXTRA_DAYS = Map.of("NORTH", 3, "NORTHEAST", 2);

    static final BigDecimal COURIER_MAX_KG = new BigDecimal("5");
    static final BigDecimal COURIER_PRICE = new BigDecimal("18.00");
    static final BigDecimal MINUS50_MINIMUM = new BigDecimal("300.00");
    static final BigDecimal COUPON_CAP_RATE = new BigDecimal("0.30");
    static final BigDecimal BANK_SLIP_MAXIMUM = new BigDecimal("1000.00");
    static final BigDecimal BANK_SLIP_FEE = new BigDecimal("3.49");
    static final BigDecimal GIFT_ABOVE = new BigDecimal("500.00");
    static final BigDecimal MONTHLY_INTEREST = new BigDecimal("0.0199");
    static final MathContext PRECISION = new MathContext(50);

    private Calculator() {}

    public static Result calculate(Purchase purchase, Context context) {
        try {
            return summarize(purchase, context);
        } catch (Refused refused) {
            return refused.refusal;
        }
    }

    // ------------------------------------------------------------------ the flow, in the prompt's order

    private static Summary summarize(Purchase p, Context ctx) {
        checkCart(p, ctx);                                           // refusals 1 to 4
        checkCodes(p);                                               // refusals 5 to 8
        Shipping shipping = quoteShipping(p, ctx);                   // refusals 9 and 10
        checkCoupon(p, ctx);                                         // refusals 11 and 12
        int installments = checkPayment(p);                          // refusals 13 and 14

        BigDecimal subtotal = subtotal(p, ctx);                      // 1. sum of the products
        BigDecimal discount = couponDiscount(p, ctx, subtotal, shipping.cost());  // 2. coupon discount
        BigDecimal insurance = insurance(p.region(), subtotal);      // 4. insurance (shipping, 3, came with the quote)
        BigDecimal orderTotal = subtotal.subtract(discount).add(shipping.cost()).add(insurance);  // 5. order total

        if (p.paymentMethod().equals("BANK_SLIP") && orderTotal.compareTo(BANK_SLIP_MAXIMUM) > 0) {
            throw refuse("PAYMENT_METHOD_UNAVAILABLE");              // refusal 15 ("passa de" = greater than)
        }
        Payment payment = payment(p, orderTotal, installments);      // 6. payment method adjustment

        return new Summary(subtotal, discount, shipping.cost(), shipping.days(), insurance,
                payment.finalTotal().subtract(orderTotal), payment.finalTotal(), installments, payment.installmentAmount(),
                clubCredit(p.clubTier(), subtotal), hasGift(p.clubTier(), subtotal));
    }

    // ------------------------------------------------------------------ the refusals

    /** 1. INVALID_ORDER, 2. UNKNOWN_PRODUCT, 3. PRODUCT_DISCONTINUED, 4. INSUFFICIENT_STOCK. */
    private static void checkCart(Purchase p, Context ctx) {
        if (p.items() == null || p.items().isEmpty() || p.customer() == null
                || isBlank(p.customer().cpf()) || isBlank(p.customer().email())) {
            throw refuse("INVALID_ORDER");
        }
        Set<String> skus = new HashSet<>();
        for (Item item : p.items()) {
            boolean invalid = item == null || isBlank(item.sku()) || item.quantity() == null || item.quantity() <= 0;
            if (invalid || !skus.add(item.sku())) throw refuse("INVALID_ORDER");   // the same SKU twice
        }
        // each of the three looks at the whole cart before moving on to the next
        if (p.items().stream().anyMatch(i -> ctx.catalog().find(i.sku()).isEmpty())) throw refuse("UNKNOWN_PRODUCT");
        if (p.items().stream().anyMatch(i -> product(ctx, i).discontinued())) throw refuse("PRODUCT_DISCONTINUED");
        if (p.items().stream().anyMatch(i -> i.quantity() > product(ctx, i).stock())) throw refuse("INSUFFICIENT_STOCK");
    }

    /** 5. INVALID_CLUB_TIER, 6. INVALID_REGION, 7. INVALID_ZIP_CODE, 8. INVALID_SHIPPING_METHOD. */
    private static void checkCodes(Purchase p) {
        if (!CLUB_TIERS.contains(p.clubTier())) throw refuse("INVALID_CLUB_TIER");
        if (!INSURANCE.containsKey(p.region())) throw refuse("INVALID_REGION");
        if (p.zipCode() == null || !p.zipCode().matches("\\d{8}")) throw refuse("INVALID_ZIP_CODE");
        if (!SHIPPING_METHODS.contains(p.shippingMethod())) throw refuse("INVALID_SHIPPING_METHOD");
    }

    /** The shipping cost and days. 9. SHIPPING_METHOD_UNAVAILABLE, 10. CARRIER_UNAVAILABLE. */
    private static Shipping quoteShipping(Purchase p, Context ctx) {
        BigDecimal weight = weightKg(p, ctx);
        Shipping shipping = switch (p.shippingMethod()) {
            case "COURIER" -> {
                if (weight.compareTo(COURIER_MAX_KG) > 0) throw refuse("SHIPPING_METHOD_UNAVAILABLE");
                yield new Shipping(COURIER_PRICE, 0);
            }
            case "STORE_PICKUP" -> new Shipping(round(BigDecimal.ZERO), 1);
            default -> quoteTransLog(p, ctx, weight);                 // ECONOMY and EXPRESS
        };
        // GOLD pays no shipping, except with the courier (it is outsourced)
        boolean goldFreeShipping = p.clubTier().equals("GOLD") && !p.shippingMethod().equals("COURIER");
        return goldFreeShipping ? new Shipping(round(BigDecimal.ZERO), shipping.days()) : shipping;
    }

    private static Shipping quoteTransLog(Purchase p, Context ctx, BigDecimal weightKg) {
        int grams = weightKg.movePointRight(3).intValueExact();       // DECISION: exact (the weights have 2 decimals)
        String service = p.shippingMethod().equals("ECONOMY") ? "STD" : "EXP";
        return switch (ctx.transLog().quote(p.zipCode(), grams, service)) {
            case TransLog.ZipNotServed notServed -> throw refuse("SHIPPING_METHOD_UNAVAILABLE");
            case TransLog.Unavailable unavailable -> throw new Refused(new Refusal("CARRIER_UNAVAILABLE", 503));
            case TransLog.Quote quote -> new Shipping(
                    BigDecimal.valueOf(quote.priceCents(), 2),                                   // cents -> reais
                    Math.ceilDiv(quote.etaHours(), 24) + EXTRA_DAYS.getOrDefault(p.region(), 0));  // hours -> days, rounded up
        };
    }

    /** 11. INVALID_COUPON, 12. COUPON_NOT_APPLICABLE. */
    private static void checkCoupon(Purchase p, Context ctx) {
        String coupon = p.coupon();
        if (coupon == null) return;                                   // no coupon
        if (!COUPONS.contains(coupon)) throw refuse("INVALID_COUPON"); // including the empty text
        boolean notApplicable = switch (coupon) {
            case "WELCOME10" -> ctx.hasPaidOrder().test(p.customer().cpf());     // only on the first purchase
            case "MINUS50" -> subtotal(p, ctx).compareTo(MINUS50_MINIMUM) < 0;    // from R$ 300.00 on
            case "FREESHIPPING" -> p.shippingMethod().equals("STORE_PICKUP");    // store pickup has no shipping
            default -> false;                                         // BUY3PAY2 without a trio applies, with zero discount
        };
        if (notApplicable) throw refuse("COUPON_NOT_APPLICABLE");
    }

    /** 13. INVALID_PAYMENT_METHOD, 14. INVALID_INSTALLMENTS. Returns the number of installments. */
    private static int checkPayment(Purchase p) {
        if (!PAYMENT_METHODS.contains(p.paymentMethod())) throw refuse("INVALID_PAYMENT_METHOD");
        int installments = p.installments() == null ? 1 : p.installments();
        boolean allowed = p.paymentMethod().equals("CREDIT_CARD") ? installments >= 1 && installments <= 12 : installments == 1;
        if (!allowed) throw refuse("INVALID_INSTALLMENTS");
        return installments;
    }

    // ------------------------------------------------------------------ the calculation

    static BigDecimal subtotal(Purchase p, Context ctx) {
        return p.items().stream()
                .map(i -> round(product(ctx, i).price().multiply(BigDecimal.valueOf(i.quantity()))))
                .reduce(round(BigDecimal.ZERO), BigDecimal::add);
    }

    static BigDecimal weightKg(Purchase p, Context ctx) {
        return p.items().stream()
                .map(i -> product(ctx, i).weightKg().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);           // not rounded
    }

    /** The coupon discount, already capped at 30% of the products (the cap applies to every coupon). */
    static BigDecimal couponDiscount(Purchase p, Context ctx, BigDecimal subtotal, BigDecimal shippingCost) {
        return rawCouponDiscount(p, ctx, subtotal, shippingCost).min(couponCap(subtotal));
    }

    /** The coupon discount before the 30% cap. */
    static BigDecimal rawCouponDiscount(Purchase p, Context ctx, BigDecimal subtotal, BigDecimal shippingCost) {
        if (p.coupon() == null) return round(BigDecimal.ZERO);
        return switch (p.coupon()) {
            case "WELCOME10" -> round(subtotal.multiply(new BigDecimal("0.10")));
            case "MINUS50" -> new BigDecimal("50.00");
            case "FREESHIPPING" -> round(shippingCost);              // the discount is the shipping itself
            case "BUY3PAY2" -> p.items().stream()                     // every 3 units of a product, 1 is free
                    .map(i -> round(product(ctx, i).price().multiply(BigDecimal.valueOf(i.quantity() / 3))))
                    .reduce(round(BigDecimal.ZERO), BigDecimal::add);
            default -> throw new IllegalStateException(p.coupon());
        };
    }

    static BigDecimal couponCap(BigDecimal subtotal) {
        return round(subtotal.multiply(COUPON_CAP_RATE));
    }

    /** The region's rate on the products; if the region has a limit and the result exceeds it, the limit. */
    static BigDecimal insurance(String region, BigDecimal subtotal) {
        InsuranceRule rule = INSURANCE.get(region);
        BigDecimal value = round(subtotal.multiply(rule.rate()));
        return rule.limit() != null && value.compareTo(rule.limit()) > 0 ? rule.limit() : value;
    }

    private static Payment payment(Purchase p, BigDecimal orderTotal, int installments) {
        return switch (p.paymentMethod()) {
            case "PIX" -> {
                // DECISION: the Pix discount is only for who did not give a coupon (even a zero-discount one)
                BigDecimal pixDiscount = p.coupon() == null ? round(orderTotal.multiply(new BigDecimal("0.05"))) : round(BigDecimal.ZERO);
                BigDecimal total = orderTotal.subtract(pixDiscount);
                yield new Payment(total, total);
            }
            case "BANK_SLIP" -> {
                BigDecimal total = orderTotal.add(BANK_SLIP_FEE);
                yield new Payment(total, total);
            }
            default -> installments <= 3
                    // no interest: the total is the order's, and each installment is the total divided
                    ? new Payment(orderTotal, orderTotal.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_EVEN))
                    // with interest: the Price table, and the total is the installment times their number
                    : priceTable(orderTotal, installments);
        };
    }

    /** Price table: installment = total × rate ÷ (1 − (1 + rate)^−n), rounded to cents. */
    static Payment priceTable(BigDecimal orderTotal, int installments) {
        BigDecimal growth = BigDecimal.ONE.add(MONTHLY_INTEREST).pow(installments, PRECISION);              // (1 + rate)^n
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(growth, PRECISION));             // 1 − (1 + rate)^−n
        BigDecimal amount = round(orderTotal.multiply(MONTHLY_INTEREST).divide(divisor, PRECISION));
        return new Payment(amount.multiply(BigDecimal.valueOf(installments)), amount);
    }

    static BigDecimal clubCredit(String clubTier, BigDecimal subtotal) {
        return switch (clubTier) {
            case "SILVER" -> round(subtotal.multiply(new BigDecimal("0.02")));
            case "GOLD" -> round(subtotal.multiply(new BigDecimal("0.05")));
            default -> round(BigDecimal.ZERO);
        };
    }

    /** The gift: GOLD with products above R$ 500.00 (greater than, not greater or equal). */
    static boolean hasGift(String clubTier, BigDecimal subtotal) {
        return clubTier.equals("GOLD") && subtotal.compareTo(GIFT_ABOVE) > 0;
    }

    // ------------------------------------------------------------------ small pieces

    record Shipping(BigDecimal cost, int days) {}

    record Payment(BigDecimal finalTotal, BigDecimal installmentAmount) {}

    /** A refusal stops the calculation; {@link #calculate} returns it as the result. */
    private static final class Refused extends RuntimeException {
        final Refusal refusal;

        Refused(Refusal refusal) {
            super(refusal.error(), null, false, false);
            this.refusal = refusal;
        }
    }

    private static Refused refuse(String error) {
        return new Refused(new Refusal(error, 422));
    }

    /** Rounds to cents, half to even. */
    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static Product product(Context ctx, Item item) {
        return ctx.catalog().find(item.sku()).orElseThrow();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
