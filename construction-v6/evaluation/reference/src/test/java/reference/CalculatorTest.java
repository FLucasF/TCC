package reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import reference.Calculator.Context;
import reference.Purchase.Customer;
import reference.Purchase.Item;
import reference.Result.Refusal;
import reference.Result.Summary;

/**
 * The new v2 rules and the exact boundaries, which the (random) differential test does not reach.
 * Each test says in its name the prompt rule it checks.
 */
class CalculatorTest {

    /** A fixed TransLog: it always answers the same, so the test controls the shipping cost and days. */
    static TransLog transLog(long cents, int hours) {
        return (zip, grams, service) -> new TransLog.Quote(cents, hours);
    }

    /** A test catalog with "round" prices, to reach the exact boundaries. */
    static final Catalog ROUND_PRICES = new Catalog(List.of(
            Catalog.product("P100", "Produto 100", "100.00", "1.00", 99, false),
            Catalog.product("P300", "Produto 300", "300.00", "1.00", 99, false),
            Catalog.product("P500", "Produto 500", "500.00", "1.00", 99, false),
            Catalog.product("P500C", "Produto 500,01", "500.01", "1.00", 99, false),
            Catalog.product("P299", "Produto 299,99", "299.99", "1.00", 99, false),
            Catalog.product("P1200", "Produto 1200", "1200.00", "1.00", 99, false),
            Catalog.product("P1500", "Produto 1500", "1500.00", "1.00", 99, false),
            Catalog.product("P10", "Produto 10", "10.00", "0.10", 99, false),
            Catalog.product("P_LIGHT", "Produto leve", "20.00", "2.50", 99, false)));

    /** A valid starting purchase (2 shirts + 1 sneaker, EXPRESS, credit card 1x, BRONZE, SOUTHEAST), which each test changes. */
    static class Draft {
        Customer customer = new Customer("12345678909", "ana@exemplo.com");
        List<Item> items = List.of(new Item("CAM-001", 2), new Item("TEN-001", 1));
        String zipCode = "01310100", region = "SOUTHEAST", shipping = "EXPRESS", coupon = null, payment = "CREDIT_CARD", tier = "BRONZE";
        Integer installments = 1;
        Catalog catalog = Catalog.initial();
        boolean hasPaidOrder = false;
        TransLog transLog = transLog(3310, 41);

        Result calculate() {
            Purchase p = new Purchase(customer, items, zipCode, region, shipping, coupon, payment, installments, tier);
            return Calculator.calculate(p, new Context(catalog, cpf -> hasPaidOrder, transLog));
        }
    }

    static Summary summary(Consumer<Draft> change) {
        Draft d = new Draft();
        change.accept(d);
        return assertInstanceOf(Summary.class, d.calculate());
    }

    static void refused(String error, int http, Consumer<Draft> change) {
        Draft d = new Draft();
        change.accept(d);
        Refusal r = assertInstanceOf(Refusal.class, d.calculate());
        assertEquals(error, r.error());
        assertEquals(http, r.http());
    }

    static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @Nested
    class ForeignTransLog {
        @Test void centsBecomeReais() { assertEquals(money("33.10"), summary(d -> {}).shippingCost()); }

        @Test void fortyOneHoursAreTwoDays() { assertEquals(2, summary(d -> d.transLog = transLog(3310, 41)).deliveryDays()); }

        @Test void fortyEightHoursAreTwoDays() { assertEquals(2, summary(d -> d.transLog = transLog(3310, 48)).deliveryDays()); }

        @Test void fortyNineHoursAreThreeDays() { assertEquals(3, summary(d -> d.transLog = transLog(3310, 49)).deliveryDays()); }

        @Test void asksForTheServiceAndTheWeightInGrams() {
            String[] seen = new String[1];
            summary(d -> d.transLog = (zip, grams, service) -> { seen[0] = zip + "|" + grams + "|" + service; return new TransLog.Quote(1000, 24); });
            assertEquals("01310100|1800|EXP", seen[0]);   // 2 x 0.30 kg + 1.20 kg = 1,800 g
        }

        @Test void zipNotServedMeansShippingMethodUnavailable() {
            refused("SHIPPING_METHOD_UNAVAILABLE", 422, d -> d.transLog = (z, g, s) -> new TransLog.ZipNotServed());
        }

        @Test void unavailableIs503() {
            refused("CARRIER_UNAVAILABLE", 503, d -> d.transLog = (z, g, s) -> new TransLog.Unavailable());
        }

        @Test void courierWorksWithTheTransLogDown() {
            assertEquals(money("18.00"), summary(d -> { d.shipping = "COURIER"; d.transLog = (z, g, s) -> new TransLog.Unavailable(); }).shippingCost());
        }

        @Test void storePickupWorksWithTheTransLogDown() {
            assertEquals(1, summary(d -> { d.shipping = "STORE_PICKUP"; d.transLog = (z, g, s) -> new TransLog.Unavailable(); }).deliveryDays());
        }
    }

    @Nested
    class ExtraDays {
        @Test void northHasThreeExtraDaysWithTheTransLog() { assertEquals(5, summary(d -> d.region = "NORTH").deliveryDays()); }

        @Test void northeastHasTwoExtraDaysWithTheTransLog() { assertEquals(4, summary(d -> d.region = "NORTHEAST").deliveryDays()); }

        @Test void courierInTheNorthHasNoExtraDays() {
            assertEquals(0, summary(d -> { d.region = "NORTH"; d.shipping = "COURIER"; }).deliveryDays());
        }

        @Test void storePickupInTheNortheastHasNoExtraDays() {
            assertEquals(1, summary(d -> { d.region = "NORTHEAST"; d.shipping = "STORE_PICKUP"; }).deliveryDays());
        }
    }

    @Nested
    class InsuranceLimit {
        @Test void northAboveTheLimitStaysAtTheLimit() {        // 2.5% of 1,500.00 = 37.50
            assertEquals(money("30.00"), summary(d -> { d.catalog = ROUND_PRICES; d.items = List.of(new Item("P1500", 1)); d.region = "NORTH"; }).insurance());
        }

        @Test void northExactlyAtTheLimit() {                    // 2.5% of 1,200.00 = 30.00
            assertEquals(money("30.00"), summary(d -> { d.catalog = ROUND_PRICES; d.items = List.of(new Item("P1200", 1)); d.region = "NORTH"; }).insurance());
        }

        @Test void southeastHasNoLimit() {                       // 1% of 1,500.00
            assertEquals(money("15.00"), summary(d -> { d.catalog = ROUND_PRICES; d.items = List.of(new Item("P1500", 1)); }).insurance());
        }
    }

    @Nested
    class ClubAndShipping {
        @Test void goldPaysNoShippingWithExpress() { assertEquals(money("0.00"), summary(d -> d.tier = "GOLD").shippingCost()); }

        @Test void goldPaysTheCourier() {
            assertEquals(money("18.00"), summary(d -> { d.tier = "GOLD"; d.shipping = "COURIER"; }).shippingCost());
        }

        @Test void freeShippingWithGoldGivesZeroDiscount() {
            assertEquals(money("0.00"), summary(d -> { d.tier = "GOLD"; d.coupon = "FREESHIPPING"; }).couponDiscount());
        }

        @Test void noGiftWithExactlyFiveHundred() {
            assertEquals(false, summary(d -> { d.catalog = ROUND_PRICES; d.items = List.of(new Item("P500", 1)); d.tier = "GOLD"; }).gift());
        }

        @Test void giftWithFiveHundredAndOneCent() {
            assertEquals(true, summary(d -> { d.catalog = ROUND_PRICES; d.items = List.of(new Item("P500C", 1)); d.tier = "GOLD"; }).gift());
        }
    }

    @Nested
    class Coupons {
        @Test void freeShippingDoesNotApplyToStorePickup() {
            refused("COUPON_NOT_APPLICABLE", 422, d -> { d.coupon = "FREESHIPPING"; d.shipping = "STORE_PICKUP"; });
        }

        @Test void welcomeDoesNotApplyToASecondPurchase() {
            refused("COUPON_NOT_APPLICABLE", 422, d -> { d.coupon = "WELCOME10"; d.hasPaidOrder = true; });
        }

        @Test void welcomeAppliesToTheFirstPurchase() {
            assertEquals(money("40.97"), summary(d -> d.coupon = "WELCOME10").couponDiscount());
        }

        @Test void minus50AppliesWithExactlyThreeHundred() {
            assertEquals(money("50.00"), summary(d -> { d.catalog = ROUND_PRICES; d.items = List.of(new Item("P300", 1)); d.coupon = "MINUS50"; }).couponDiscount());
        }

        @Test void minus50DoesNotApplyBelowThreeHundred() {
            refused("COUPON_NOT_APPLICABLE", 422, d -> { d.catalog = ROUND_PRICES; d.items = List.of(new Item("P299", 1)); d.coupon = "MINUS50"; });
        }

        @Test void buy3Pay2WithSevenUnitsGivesTwoFree() {       // 7 socks: 2 free = 39.80 (below 30% of 139.30 = 41.79)
            assertEquals(money("39.80"), summary(d -> { d.items = List.of(new Item("MEI-001", 7)); d.coupon = "BUY3PAY2"; }).couponDiscount());
        }

        @Test void buy3Pay2WithoutATrioGivesZero() {
            assertEquals(money("0.00"), summary(d -> d.coupon = "BUY3PAY2").couponDiscount());
        }

        @Test void thirtyPercentCapOnBuy3Pay2() {                // 3 shirts: 1 free = 79.90; 30% of 239.70 = 71.91
            assertEquals(money("71.91"), summary(d -> { d.items = List.of(new Item("CAM-001", 3)); d.coupon = "BUY3PAY2"; }).couponDiscount());
        }

        @Test void thirtyPercentCapOnFreeShipping() {            // 1 sock (19.90) with 33.10 shipping: 30% of 19.90 = 5.97
            assertEquals(money("5.97"), summary(d -> { d.items = List.of(new Item("MEI-001", 1)); d.coupon = "FREESHIPPING"; }).couponDiscount());
        }

        @Test void freeShippingBelowTheCapDiscountsTheWholeShipping() {
            assertEquals(money("33.10"), summary(d -> d.coupon = "FREESHIPPING").couponDiscount());
        }
    }

    @Nested
    class Payment {
        @Test void pixWithoutCouponHasFivePercentOff() {         // total 409.70 + 33.10 + 4.10 = 446.90; 5% = 22.345, which becomes 22.34 (half to even)
            assertEquals(money("-22.34"), summary(d -> d.payment = "PIX").paymentAdjustment());
        }

        @Test void pixWithCouponHasNoDiscount() {
            assertEquals(money("0.00"), summary(d -> { d.payment = "PIX"; d.coupon = "WELCOME10"; }).paymentAdjustment());
        }

        @Test void pixWithAZeroDiscountCouponAlsoHasNoDiscount() {
            assertEquals(money("0.00"), summary(d -> { d.payment = "PIX"; d.coupon = "BUY3PAY2"; }).paymentAdjustment());
        }

        @Test void bankSlipWithExactlyOneThousandIsAccepted() {  // 900.00 + 91.00 shipping + 9.00 insurance = exactly 1,000.00
            Summary s = summary(d -> {
                d.catalog = ROUND_PRICES; d.items = List.of(new Item("P100", 9)); d.payment = "BANK_SLIP"; d.transLog = transLog(9100, 24);
            });
            assertEquals(money("1003.49"), s.finalTotal());
        }

        @Test void bankSlipWithOneThousandAndOneCentIsRefused() {
            refused("PAYMENT_METHOD_UNAVAILABLE", 422, d -> {
                d.catalog = ROUND_PRICES; d.items = List.of(new Item("P100", 9)); d.payment = "BANK_SLIP"; d.transLog = transLog(9101, 24);
            });
        }

        @Test void creditCardSixTimesLikeExampleTwoOfV5() {      // total 431.45 -> 6x of 77.00
            Summary s = summary(d -> {
                d.shipping = "ECONOMY"; d.installments = 6; d.tier = "SILVER"; d.region = "MIDWEST"; d.transLog = transLog(1560, 168);
            });
            assertEquals(money("77.00"), s.installmentAmount());
            assertEquals(money("462.00"), s.finalTotal());
        }
    }

    @Nested
    class RefusalOrder {
        @Test void repeatedSkuIsInvalidOrder() {
            refused("INVALID_ORDER", 422, d -> d.items = List.of(new Item("CAM-001", 1), new Item("CAM-001", 2)));
        }

        @Test void missingEmailIsInvalidOrder() { refused("INVALID_ORDER", 422, d -> d.customer = new Customer("12345678909", null)); }

        @Test void discontinuedProduct() { refused("PRODUCT_DISCONTINUED", 422, d -> d.items = List.of(new Item("REL-001", 1))); }

        @Test void outOfStock() { refused("INSUFFICIENT_STOCK", 422, d -> d.items = List.of(new Item("BON-001", 1))); }

        @Test void exactStockIsAccepted() { summary(d -> d.items = List.of(new Item("MOC-001", 1))); }

        @Test void unknownComesBeforeDiscontinued() {
            refused("UNKNOWN_PRODUCT", 422, d -> d.items = List.of(new Item("REL-001", 1), new Item("XYZ-999", 1)));
        }

        @Test void tierComesBeforeRegion() { refused("INVALID_CLUB_TIER", 422, d -> { d.tier = "DIAMOND"; d.region = "EAST"; }); }

        @Test void zipCodeWithADashIsInvalid() { refused("INVALID_ZIP_CODE", 422, d -> d.zipCode = "01310-100"); }

        @Test void zipCodeComesBeforeShippingMethod() { refused("INVALID_ZIP_CODE", 422, d -> { d.zipCode = null; d.shipping = "DRONE"; }); }

        @Test void courierWithExactlyFiveKilosIsAccepted() {     // 2 x 2.50 kg
            assertEquals(money("18.00"), summary(d -> { d.catalog = ROUND_PRICES; d.items = List.of(new Item("P_LIGHT", 2)); d.shipping = "COURIER"; }).shippingCost());
        }

        @Test void courierAboveFiveKilosIsUnavailable() {        // 2 x 2.50 + 0.10
            refused("SHIPPING_METHOD_UNAVAILABLE", 422, d -> {
                d.catalog = ROUND_PRICES; d.items = List.of(new Item("P_LIGHT", 2), new Item("P10", 1)); d.shipping = "COURIER";
            });
        }

        @Test void carrierComesBeforeInvalidCoupon() {
            refused("CARRIER_UNAVAILABLE", 503, d -> { d.coupon = "XMAS"; d.transLog = (z, g, s) -> new TransLog.Unavailable(); });
        }

        @Test void emptyCouponIsInvalid() { refused("INVALID_COUPON", 422, d -> d.coupon = ""); }

        @Test void couponComesBeforePaymentMethod() { refused("INVALID_COUPON", 422, d -> { d.coupon = "XMAS"; d.payment = "CHECK"; }); }

        @Test void installmentsWithPixAreInvalid() { refused("INVALID_INSTALLMENTS", 422, d -> { d.payment = "PIX"; d.installments = 2; }); }

        @Test void thirteenInstallmentsOnTheCardAreInvalid() { refused("INVALID_INSTALLMENTS", 422, d -> d.installments = 13); }

        @Test void missingInstallmentsMeanOne() { assertEquals(1, summary(d -> d.installments = null).installments()); }
    }
}
