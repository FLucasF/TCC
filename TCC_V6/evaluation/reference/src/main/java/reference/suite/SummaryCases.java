package reference.suite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import reference.Catalog;
import reference.Catalog.Product;
import reference.PromptExamples;
import reference.Purchase;
import reference.Purchase.Customer;
import reference.Purchase.Item;
import reference.TransLog;

/**
 * The cases for {@code POST /checkout/summary}, in three groups:
 *
 * 1. the 10 examples of the prompt;
 * 2. the exact boundaries the real catalog lets us build (5 kg, R$ 1,000.00, 48 hours, the last unit...);
 * 3. random purchases, valid and invalid, with random TransLog answers.
 *
 * The two last groups complement each other: random purchases find rounding errors (half a cent is
 * rare), and the exact boundaries find "greater than" swapped for "greater or equal" (a random purchase
 * almost never lands on a boundary). Example 9 needs a paid order first, so the suite runs it apart.
 */
public final class SummaryCases {

    public record Case(String name, Purchase purchase, TransLog.Answer transLogAnswer) {}

    static final String CPF = "12345678909";

    public static List<Case> all(int randomCount, long seed) {
        List<Case> cases = new ArrayList<>();
        cases.addAll(promptExamples());
        cases.addAll(exactBoundaries());
        cases.addAll(randomPurchases(randomCount, new Random(seed)));
        return cases;
    }

    /** The prompt examples, except number 9 (the customer already has a paid order). */
    static List<Case> promptExamples() {
        List<Case> cases = new ArrayList<>();
        for (int n = 1; n <= PromptExamples.EXAMPLES.size(); n++) {
            PromptExamples.Example e = PromptExamples.EXAMPLES.get(n - 1);
            if (e.customerHasPaidOrder()) continue;
            cases.add(new Case("example " + n, purchase(CPF, e.items(), e.shippingMethod(), e.coupon(), e.paymentMethod(), e.installments(), e.clubTier(), e.region()),
                    e.transLogAnswer()));
        }
        return cases;
    }

    /** Example 9 of the prompt, for a customer that will have a paid order before it runs. */
    static Case exampleNine(String cpfWithPaidOrder) {
        PromptExamples.Example e = PromptExamples.EXAMPLES.get(8);
        return new Case("example 9 (the customer already has a paid order)",
                purchase(cpfWithPaidOrder, e.items(), e.shippingMethod(), e.coupon(), e.paymentMethod(), e.installments(), e.clubTier(), e.region()),
                e.transLogAnswer());
    }

    static List<Case> exactBoundaries() {
        TransLog.Answer anyQuote = new TransLog.Quote(2500, 30);
        TransLog.Answer down = new TransLog.Unavailable();
        return List.of(
                boundary("courier with exactly 5.00 kg", items("TEN-001", 4, "MEI-001", 2), "COURIER", null, "PIX", null, "BRONZE", "SOUTHEAST", null),
                boundary("courier with 5.10 kg", items("TEN-001", 4, "MEI-001", 3), "COURIER", null, "PIX", null, "BRONZE", "SOUTHEAST", null),
                // 889.70 of products + 8.90 of insurance + the chosen shipping = exactly 1,000.00 (or 1,000.01)
                boundary("bank slip with exactly 1,000.00", items("TEN-001", 2, "JAQ-001", 1), "ECONOMY", null, "BANK_SLIP", null, "BRONZE", "SOUTHEAST", new TransLog.Quote(10140, 100)),
                boundary("bank slip with 1,000.01", items("TEN-001", 2, "JAQ-001", 1), "ECONOMY", null, "BANK_SLIP", null, "BRONZE", "SOUTHEAST", new TransLog.Quote(10141, 100)),
                boundary("48 hours = 2 days", items("CAM-001", 1), "EXPRESS", null, "PIX", null, "BRONZE", "SOUTH", new TransLog.Quote(1990, 48)),
                boundary("49 hours = 3 days", items("CAM-001", 1), "EXPRESS", null, "PIX", null, "BRONZE", "SOUTH", new TransLog.Quote(1990, 49)),
                boundary("1 hour = 1 day", items("CAM-001", 1), "EXPRESS", null, "PIX", null, "BRONZE", "SOUTH", new TransLog.Quote(1990, 1)),
                boundary("exact stock (the last backpack)", items("MOC-001", 1), "STORE_PICKUP", null, "PIX", null, "BRONZE", "SOUTHEAST", null),
                boundary("not enough stock (2 backpacks)", items("MOC-001", 2), "STORE_PICKUP", null, "PIX", null, "BRONZE", "SOUTHEAST", null),
                boundary("out of stock", items("BON-001", 1), "STORE_PICKUP", null, "PIX", null, "BRONZE", "SOUTHEAST", null),
                boundary("discontinued product", items("REL-001", 1), "STORE_PICKUP", null, "PIX", null, "BRONZE", "SOUTHEAST", null),
                boundary("TransLog down with the courier (not needed)", items("CAM-001", 1), "COURIER", null, "PIX", null, "BRONZE", "SOUTHEAST", down),
                boundary("TransLog down with express", items("CAM-001", 1), "EXPRESS", null, "PIX", null, "BRONZE", "SOUTHEAST", down),
                boundary("30% cap on FREESHIPPING", items("MEI-001", 1), "EXPRESS", "FREESHIPPING", "CREDIT_CARD", 1, "BRONZE", "SOUTHEAST", new TransLog.Quote(3310, 41)),
                boundary("30% cap on BUY3PAY2", items("CAM-001", 3), "STORE_PICKUP", "BUY3PAY2", "CREDIT_CARD", 1, "BRONZE", "SOUTHEAST", null),
                boundary("BUY3PAY2 without a trio gives zero", items("CAM-001", 2), "STORE_PICKUP", "BUY3PAY2", "PIX", null, "BRONZE", "SOUTHEAST", null),
                boundary("empty coupon is invalid", items("CAM-001", 1), "STORE_PICKUP", "", "PIX", null, "BRONZE", "SOUTHEAST", null),
                boundary("GOLD pays the courier", items("CAM-001", 1), "COURIER", null, "CREDIT_CARD", 1, "GOLD", "SOUTHEAST", null),
                boundary("FREESHIPPING with GOLD gives zero", items("CAM-001", 1), "EXPRESS", "FREESHIPPING", "CREDIT_CARD", 1, "GOLD", "SOUTHEAST", anyQuote),
                boundary("repeated SKU", items("CAM-001", 1, "CAM-001", 2), "STORE_PICKUP", null, "PIX", null, "BRONZE", "SOUTHEAST", null),
                new Case("zip code with a dash", new Purchase(new Customer(CPF, "ana@exemplo.com"), items("CAM-001", 1), "01310-100", "SOUTHEAST", "STORE_PICKUP", null, "PIX", null, "BRONZE"), null),
                new Case("no e-mail", new Purchase(new Customer(CPF, null), items("CAM-001", 1), "01310100", "SOUTHEAST", "STORE_PICKUP", null, "PIX", null, "BRONZE"), null),
                boundary("12 installments on the card (the maximum)", items("TEN-001", 1), "STORE_PICKUP", null, "CREDIT_CARD", 12, "BRONZE", "SOUTHEAST", null),
                boundary("13 installments on the card", items("TEN-001", 1), "STORE_PICKUP", null, "CREDIT_CARD", 13, "BRONZE", "SOUTHEAST", null),
                boundary("4 installments (the first with interest)", items("TEN-001", 1), "STORE_PICKUP", null, "CREDIT_CARD", 4, "BRONZE", "SOUTHEAST", null),
                boundary("order: tier before region", items("CAM-001", 1), "STORE_PICKUP", null, "PIX", null, "DIAMOND", "EAST", null),
                boundary("order: carrier before invalid coupon", items("CAM-001", 1), "EXPRESS", "XMAS", "PIX", null, "BRONZE", "SOUTHEAST", down));
    }

    static List<Case> randomPurchases(int count, Random r) {
        List<Product> forSale = Catalog.initial().all().stream().filter(p -> !p.discontinued() && p.stock() > 0).toList();
        List<Case> cases = new ArrayList<>();
        while (cases.size() < count) {
            List<Product> shuffled = new ArrayList<>(forSale);
            Collections.shuffle(shuffled, r);
            List<Item> items = new ArrayList<>();
            for (Product p : shuffled.subList(0, 1 + r.nextInt(3))) items.add(new Item(p.sku(), 1 + r.nextInt(Math.min(p.stock(), 6))));
            String tier = pick(r, "BRONZE", "SILVER", "GOLD"), region = pick(r, "SOUTHEAST", "SOUTH", "MIDWEST", "NORTH", "NORTHEAST");
            String shipping = pick(r, "ECONOMY", "EXPRESS", "STORE_PICKUP", "COURIER");
            String coupon = pick(r, null, null, "WELCOME10", "MINUS50", "FREESHIPPING", "BUY3PAY2"), payment = pick(r, "PIX", "CREDIT_CARD", "CREDIT_CARD", "BANK_SLIP");
            Integer installments = payment.equals("CREDIT_CARD") ? Integer.valueOf(1 + r.nextInt(12)) : null;
            String zipCode = "01310100";
            switch (r.nextInt(14)) {                                    // sometimes, one defect (a refusal)
                case 0 -> tier = "DIAMOND";
                case 1 -> region = "EAST";
                case 2 -> zipCode = "1310100";
                case 3 -> shipping = "DRONE";
                case 4 -> coupon = "XMAS";
                case 5 -> payment = "CHECK";
                case 6 -> installments = payment.equals("CREDIT_CARD") ? 13 : 2;
                case 7 -> items.set(0, new Item("XYZ-999", 1));
                default -> { }
            }
            TransLog.Answer answer = switch (r.nextInt(20)) {
                case 0, 1 -> new TransLog.ZipNotServed();
                case 2 -> new TransLog.Unavailable();
                default -> new TransLog.Quote(500 + r.nextInt(9500), 1 + r.nextInt(200));
            };
            cases.add(new Case("random " + (cases.size() + 1),
                    new Purchase(new Customer(CPF, "ana@exemplo.com"), items, zipCode, region, shipping, coupon, payment, installments, tier), answer));
        }
        return cases;
    }

    // ------------------------------------------------------------------ small pieces

    private static Case boundary(String name, List<Item> items, String shipping, String coupon, String payment, Integer installments,
                                 String tier, String region, TransLog.Answer answer) {
        return new Case("boundary: " + name, purchase(CPF, items, shipping, coupon, payment, installments, tier, region), answer);
    }

    static Purchase purchase(String cpf, List<Item> items, String shipping, String coupon, String payment, Integer installments, String tier, String region) {
        return new Purchase(new Customer(cpf, cpf + "@exemplo.com"), items, PromptExamples.ZIP_CODE, region, shipping, coupon, payment, installments, tier);
    }

    static List<Item> items(Object... skuAndQuantity) {
        return PromptExamples.items(skuAndQuantity);
    }

    @SafeVarargs
    private static <T> T pick(Random r, T... options) {
        return options[r.nextInt(options.length)];
    }
}
