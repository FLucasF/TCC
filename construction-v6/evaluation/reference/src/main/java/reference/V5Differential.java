package reference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import reference.Calculator.Context;
import reference.Catalog.Product;
import reference.Purchase.Customer;
import reference.Purchase.Item;
import reference.Result.Refusal;
import reference.Result.Summary;

/**
 * Differential test against V5's reference (TCC_V5/evaluation/acceptance-prototype/ref-strategy.mjs).
 *
 * Generates random purchases ONLY where the v2 and V5 rules are the same and prints, per purchase, one JSON
 * line: the purchase in V5's format and the v2 reference's answer. differential.mjs runs V5's reference on
 * the same purchases and compares. This way the shared math (interest, rounding, coupons, insurance)
 * inherits V5's validation.
 *
 * Left out, because they changed on purpose in v2: Pix with a coupon, FREESHIPPING with store pickup, GOLD
 * with the courier, the extra days of ECONOMY and EXPRESS in the NORTH and NORTHEAST, and the cases where
 * the 30% coupon cap or the insurance limit change the value. The fake TransLog mimics V5's shipping formulas.
 *
 * Usage: java -cp target/classes reference.V5Differential <how-many> <seed> | node differential.mjs
 */
public final class V5Differential {

    /** The fake TransLog with V5's formulas: economy 12.00 + 2.00/kg in 7 days; express 25.00 + 4.50/kg in 2 days. */
    static final TransLog V5_FORMULAS = (zip, grams, service) -> {
        BigDecimal kg = BigDecimal.valueOf(grams, 3);
        boolean economy = service.equals("STD");
        BigDecimal price = economy
                ? new BigDecimal("12.00").add(Calculator.round(kg.multiply(new BigDecimal("2"))))
                : new BigDecimal("25.00").add(Calculator.round(kg.multiply(new BigDecimal("4.5"))));
        return new TransLog.Quote(price.movePointRight(2).longValueExact(), economy ? 7 * 24 : 2 * 24);
    };

    /** The v2 API is in English; V5's reference only knows the Portuguese codes. */
    static final Map<String, String> TO_V5 = Map.ofEntries(
            Map.entry("GOLD", "OURO"), Map.entry("SILVER", "PRATA"), Map.entry("BRONZE", "BRONZE"),
            Map.entry("SOUTHEAST", "SUDESTE"), Map.entry("SOUTH", "SUL"), Map.entry("MIDWEST", "CENTRO_OESTE"),
            Map.entry("NORTH", "NORTE"), Map.entry("NORTHEAST", "NORDESTE"),
            Map.entry("ECONOMY", "ECONOMICA"), Map.entry("EXPRESS", "EXPRESSA"),
            Map.entry("STORE_PICKUP", "RETIRADA_LOJA"), Map.entry("COURIER", "MOTOBOY"),
            Map.entry("WELCOME10", "BEMVINDO10"), Map.entry("MINUS50", "MENOS50"),
            Map.entry("FREESHIPPING", "FRETEGRATIS"), Map.entry("BUY3PAY2", "LEVE3PAGUE2"),
            Map.entry("PIX", "PIX"), Map.entry("CREDIT_CARD", "CARTAO"), Map.entry("BANK_SLIP", "BOLETO"),
            Map.entry("COUPON_NOT_APPLICABLE", "CUPOM_NAO_APLICAVEL"),
            Map.entry("PAYMENT_METHOD_UNAVAILABLE", "FORMA_PAGAMENTO_INDISPONIVEL"),
            Map.entry("SHIPPING_METHOD_UNAVAILABLE", "MODALIDADE_INDISPONIVEL"),
            Map.entry("INVALID_INSTALLMENTS", "PARCELAMENTO_INVALIDO"));

    public static void main(String[] args) {
        int wanted = Integer.parseInt(args[0]);
        Random random = new Random(Long.parseLong(args[1]));
        Catalog catalog = Catalog.initial();
        List<Product> forSale = catalog.all().stream().filter(p -> !p.discontinued() && p.stock() > 0).toList();
        Context context = new Context(catalog, cpf -> false, V5_FORMULAS);

        int generated = 0, attempts = 0;
        while (generated < wanted && attempts < wanted * 50) {
            attempts++;
            Purchase purchase = randomPurchase(random, forSale);
            if (!sameRulesAsV5(purchase)) continue;
            Result result = Calculator.calculate(purchase, context);
            if (result instanceof Summary s && changedByACap(purchase, context, s)) continue;
            System.out.println("{\"v5\":" + v5Json(purchase, catalog) + ",\"v2\":" + v5StyleJson(result) + "}");
            generated++;
        }
        System.err.println(generated + " purchases generated in " + attempts + " attempts");
    }

    static Purchase randomPurchase(Random r, List<Product> forSale) {
        List<Product> shuffled = new ArrayList<>(forSale);
        Collections.shuffle(shuffled, r);
        List<Item> items = new ArrayList<>();
        for (Product p : shuffled.subList(0, 1 + r.nextInt(4))) items.add(new Item(p.sku(), 1 + r.nextInt(Math.min(p.stock(), 8))));
        String shipping = pick(r, "ECONOMY", "EXPRESS", "STORE_PICKUP", "COURIER");
        boolean usesTransLog = shipping.equals("ECONOMY") || shipping.equals("EXPRESS");
        String region = usesTransLog ? pick(r, "SOUTHEAST", "SOUTH", "MIDWEST") : pick(r, "SOUTHEAST", "SOUTH", "MIDWEST", "NORTH", "NORTHEAST");
        String payment = pick(r, "PIX", "CREDIT_CARD", "CREDIT_CARD", "BANK_SLIP");
        int installments = payment.equals("CREDIT_CARD") ? 1 + r.nextInt(13) : (r.nextInt(10) == 0 ? 2 : 1);  // sometimes invalid
        return new Purchase(new Customer("12345678909", "ana@exemplo.com"), items, "01310100", region, shipping,
                pick(r, null, null, "WELCOME10", "MINUS50", "FREESHIPPING", "BUY3PAY2"), payment, installments,
                pick(r, "BRONZE", "SILVER", "GOLD"));
    }

    /** The combinations where v2 changed the rule on purpose stay out of the comparison. */
    static boolean sameRulesAsV5(Purchase p) {
        if (p.paymentMethod().equals("PIX") && p.coupon() != null) return false;
        if ("FREESHIPPING".equals(p.coupon()) && p.shippingMethod().equals("STORE_PICKUP")) return false;
        if (p.clubTier().equals("GOLD") && p.shippingMethod().equals("COURIER")) return false;
        // A DEFECT in V5's reference (found on 2026-10-14): it sums the weights in floating point, and an
        // order of exactly 5 kg can become 5.000000000000001 kg (0.8×3 + 0.1×2 + 0.3×5 + 0.9 in JavaScript),
        // refusing the courier that the prompt says to accept. v2 sums in exact decimal. The 5 kg case of V5's
        // suite (2 × 2.50 kg) is exactly 5 in floating point too, so V5's results were not affected.
        return !(p.shippingMethod().equals("COURIER") && Calculator.weightKg(p, CONTEXT).compareTo(Calculator.COURIER_MAX_KG) == 0);
    }

    private static final Context CONTEXT = new Context(Catalog.initial(), cpf -> false, V5_FORMULAS);

    /** Did the 30% coupon cap or the insurance limit change the value? (then v2 differs from V5 on purpose) */
    static boolean changedByACap(Purchase p, Context ctx, Summary s) {
        BigDecimal raw = Calculator.rawCouponDiscount(p, ctx, s.productsSubtotal(), s.shippingCost());
        if (raw.compareTo(Calculator.couponCap(s.productsSubtotal())) > 0) return true;
        Calculator.InsuranceRule rule = Calculator.INSURANCE.get(p.region());
        BigDecimal uncapped = Calculator.round(s.productsSubtotal().multiply(rule.rate()));
        return rule.limit() != null && uncapped.compareTo(rule.limit()) > 0;
    }

    /** The purchase in V5's format (the items carry price and weight; the codes are Portuguese). */
    static String v5Json(Purchase p, Catalog catalog) {
        StringBuilder json = new StringBuilder("{\"itens\":[");
        for (int k = 0; k < p.items().size(); k++) {
            Item item = p.items().get(k);
            Product product = catalog.find(item.sku()).orElseThrow();
            json.append(k > 0 ? "," : "").append(String.format("{\"nome\":\"%s\",\"precoUnitario\":%s,\"quantidade\":%d,\"pesoKg\":%s}",
                    product.sku(), product.price().toPlainString(), item.quantity(), product.weightKg().toPlainString()));
        }
        String coupon = p.coupon() == null ? "" : "\"cupom\":\"" + toV5(p.coupon()) + "\",";
        return json.append(String.format("],\"modalidadeEntrega\":\"%s\",%s\"formaPagamento\":\"%s\",\"parcelas\":%d,\"nivelClube\":\"%s\",\"regiao\":\"%s\"}",
                toV5(p.shippingMethod()), coupon, toV5(p.paymentMethod()), p.installments(), toV5(p.clubTier()), toV5(p.region()))).toString();
    }

    /** v2's answer with V5's names and units (money in whole cents). */
    static String v5StyleJson(Result result) {
        return switch (result) {
            case Refusal r -> "{\"erro\":\"" + toV5(r.error()) + "\"}";
            case Summary s -> String.format(
                    "{\"subtotalProdutos\":%d,\"descontoCupom\":%d,\"frete\":%d,\"prazoEntregaDias\":%d,\"seguro\":%d,"
                    + "\"ajustePagamento\":%d,\"totalFinal\":%d,\"parcelas\":%d,\"valorParcela\":%d,\"creditoProximaCompra\":%d,\"brinde\":%b}",
                    cents(s.productsSubtotal()), cents(s.couponDiscount()), cents(s.shippingCost()), s.deliveryDays(), cents(s.insurance()),
                    cents(s.paymentAdjustment()), cents(s.finalTotal()), s.installments(), cents(s.installmentAmount()),
                    cents(s.nextPurchaseCredit()), s.gift());
        };
    }

    static String toV5(String code) {
        String v5 = TO_V5.get(code);
        if (v5 == null) throw new IllegalArgumentException("no V5 translation for: " + code);
        return v5;
    }

    private static long cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }

    @SafeVarargs
    private static <T> T pick(Random r, T... options) {
        return options[r.nextInt(options.length)];
    }
}
