package shop.app;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import shop.db.Database;
import shop.domain.ApiException;
import shop.domain.CartLine;
import shop.domain.ClubTier;
import shop.domain.Coupon;
import shop.domain.Money;
import shop.domain.OrderItem;
import shop.domain.PaymentMethod;
import shop.domain.Product;
import shop.domain.Region;
import shop.domain.Rules;
import shop.domain.ShippingMethod;
import shop.domain.Summary;

/** Validates a purchase and computes its summary. Shared by the summary endpoint and order registration. */
public final class CheckoutService {
    public record Checkout(String cpf, String email, List<OrderItem> items, Summary summary) {
    }

    private final Database db;
    private final Rules rules;

    public CheckoutService(Database db, Rules rules) {
        this.db = db;
        this.rules = rules;
    }

    public Checkout prepare(JsonNode body) {
        // 1. basic shape
        if (!body.isObject()) {
            throw invalid("INVALID_ORDER");
        }
        JsonNode customer = body.path("customer");
        String cpf = text(customer.path("cpf"));
        String email = text(customer.path("email"));
        List<OrderItem> items = parseItems(body.path("items"));
        if (cpf == null || email == null) {
            throw invalid("INVALID_ORDER");
        }

        // 2-4. products
        List<CartLine> lines = loadLines(items);

        // 5-8. simple fields
        ClubTier tier = rules.tiers().find(text(body.path("clubTier")))
                .orElseThrow(() -> invalid("INVALID_CLUB_TIER"));
        Region region = Region.parse(text(body.path("region"))).orElseThrow(() -> invalid("INVALID_REGION"));
        String zip = text(body.path("zipCode"));
        if (zip == null || !zip.matches("[0-9]{8}")) {
            throw invalid("INVALID_ZIP_CODE");
        }
        ShippingMethod shipping = rules.shipping().find(text(body.path("shippingMethod")))
                .orElseThrow(() -> invalid("INVALID_SHIPPING_METHOD"));

        // 9-10. shipping quote
        BigDecimal subtotal = Money.round(lines.stream().map(CartLine::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal weightKg = lines.stream().map(CartLine::weightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        ShippingMethod.Quote quote = shipping.quote(new ShippingMethod.Request(zip, weightKg, region));
        BigDecimal shippingCost = tier.freeShipping() && shipping.coveredByTierFreeShipping()
                ? Money.ZERO : Money.round(quote.cost());

        // 11-12. coupon
        JsonNode couponNode = body.path("coupon");
        boolean couponUsed = !couponNode.isMissingNode() && !couponNode.isNull();
        BigDecimal couponDiscount = Money.ZERO;
        if (couponUsed) {
            Coupon coupon = rules.coupons().find(couponNode.isTextual() ? couponNode.asText() : null)
                    .orElseThrow(() -> invalid("INVALID_COUPON"));
            Coupon.Context context = new Coupon.Context(cpf, lines, subtotal, shipping, shippingCost,
                    () -> !db.tx(d -> d.hasCompletedPurchase(cpf)));
            BigDecimal cap = Money.round(subtotal.multiply(rules.couponDiscountCap()));
            couponDiscount = Money.round(coupon.discount(context).min(cap));
        }

        // 13-15. payment
        PaymentMethod payment = rules.payments().find(text(body.path("paymentMethod")))
                .orElseThrow(() -> invalid("INVALID_PAYMENT_METHOD"));
        int installments = parseInstallments(body.path("installments"), payment);
        BigDecimal insurance = region.insurance(subtotal);
        BigDecimal orderTotal = subtotal.subtract(couponDiscount).add(shippingCost).add(insurance);
        payment.checkAvailable(orderTotal);
        PaymentMethod.Settlement settlement = payment.settle(orderTotal, installments, couponUsed);

        Summary summary = new Summary(subtotal, couponDiscount, shippingCost, quote.days(), insurance,
                Money.round(settlement.finalTotal().subtract(orderTotal)), Money.round(settlement.finalTotal()),
                installments, Money.round(settlement.installmentAmount()), tier.credit(subtotal),
                tier.gift(subtotal));
        return new Checkout(cpf, email, items, summary);
    }

    private static List<OrderItem> parseItems(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            throw invalid("INVALID_ORDER");
        }
        List<OrderItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode item : node) {
            String sku = text(item.path("sku"));
            JsonNode quantity = item.path("quantity");
            if (sku == null || !seen.add(sku) || !quantity.isIntegralNumber() || !quantity.canConvertToInt()
                    || quantity.intValue() <= 0) {
                throw invalid("INVALID_ORDER");
            }
            items.add(new OrderItem(sku, quantity.intValue()));
        }
        return items;
    }

    private List<CartLine> loadLines(List<OrderItem> items) {
        Map<String, Product> products = db.tx(d -> d.products(items.stream().map(OrderItem::sku).toList()));
        List<CartLine> lines = new ArrayList<>();
        for (OrderItem item : items) {
            Product p = products.get(item.sku());
            if (p == null) {
                throw invalid("UNKNOWN_PRODUCT");
            }
            lines.add(new CartLine(p, item.quantity()));
        }
        if (lines.stream().anyMatch(l -> l.product().discontinued())) {
            throw invalid("PRODUCT_DISCONTINUED");
        }
        if (lines.stream().anyMatch(l -> l.product().stock() < l.quantity())) {
            throw invalid("INSUFFICIENT_STOCK");
        }
        return lines;
    }

    private static int parseInstallments(JsonNode node, PaymentMethod payment) {
        int installments = 1;
        if (!node.isMissingNode() && !node.isNull()) {
            if (!node.isIntegralNumber() || !node.canConvertToInt()) {
                throw invalid("INVALID_INSTALLMENTS");
            }
            installments = node.intValue();
        }
        if (!payment.acceptsInstallments(installments)) {
            throw invalid("INVALID_INSTALLMENTS");
        }
        return installments;
    }

    /** The text of a JSON string, or null if absent, not a string or blank. */
    static String text(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }

    private static ApiException invalid(String code) {
        return ApiException.unprocessable(code);
    }
}
