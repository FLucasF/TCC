package shop;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckoutExamplesTest {
    private static final Instant NOW = Instant.parse("2026-10-14T15:30:00Z");

    static String body(String items, String ship, String coupon, String pay, int inst, String tier, String region) {
        return """
            {"customer":{"cpf":"12345678909","email":"ana@exemplo.com"},"items":%s,"zipCode":"01310100",
             "region":"%s","shippingMethod":"%s"%s,"paymentMethod":"%s","installments":%d,"clubTier":"%s"}"""
                .formatted(items, region, ship, coupon == null ? "" : ",\"coupon\":\"" + coupon + "\"", pay, inst, tier);
    }

    private static void check(TestApp.Reply r, String subtotal, String coupon, String shipping, int days,
                              String insurance, String adjustment, String total, int inst, String instAmount,
                              String credit, boolean gift) {
        assertEquals(200, r.status());
        var b = r.body();
        assertEquals(subtotal, b.get("productsSubtotal").decimalValue().toPlainString());
        assertEquals(coupon, b.get("couponDiscount").decimalValue().toPlainString());
        assertEquals(shipping, b.get("shippingCost").decimalValue().toPlainString());
        assertEquals(days, b.get("deliveryDays").asInt());
        assertEquals(insurance, b.get("insurance").decimalValue().toPlainString());
        assertEquals(adjustment, b.get("paymentAdjustment").decimalValue().toPlainString());
        assertEquals(total, b.get("finalTotal").decimalValue().toPlainString());
        assertEquals(inst, b.get("installments").asInt());
        assertEquals(instAmount, b.get("installmentAmount").decimalValue().toPlainString());
        assertEquals(credit, b.get("nextPurchaseCredit").decimalValue().toPlainString());
        assertEquals(gift, b.get("gift").asBoolean());
    }

    private static final String CAM2_TEN1 = "[{\"sku\":\"CAM-001\",\"quantity\":2},{\"sku\":\"TEN-001\",\"quantity\":1}]";

    @Test
    void examples() {
        try (TestApp app = new TestApp(NOW)) {
            app.carrier(3310, 41);
            check(app.post("/checkout/summary", body(CAM2_TEN1, "EXPRESS", "WELCOME10", "PIX", 1, "BRONZE", "NORTH")),
                    "409.70", "40.97", "33.10", 5, "10.24", "0.00", "412.07", 1, "412.07", "0.00", false);

            app.carrier(1560, 168);
            check(app.post("/checkout/summary", body(CAM2_TEN1, "ECONOMY", null, "CREDIT_CARD", 6, "SILVER", "MIDWEST")),
                    "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);

            check(app.post("/checkout/summary", body("[{\"sku\":\"FON-001\",\"quantity\":2}]", "COURIER", "MINUS50",
                            "BANK_SLIP", 1, "GOLD", "NORTHEAST")),
                    "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "19.99", false);

            check(app.post("/checkout/summary", body(
                            "[{\"sku\":\"MEI-001\",\"quantity\":7},{\"sku\":\"CAM-001\",\"quantity\":2}]",
                            "STORE_PICKUP", "BUY3PAY2", "CREDIT_CARD", 3, "SILVER", "SOUTH")),
                    "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);

            app.carrier(2475, 30);
            check(app.post("/checkout/summary", body("[{\"sku\":\"CAM-001\",\"quantity\":3}]", "EXPRESS", "BUY3PAY2",
                            "PIX", 1, "BRONZE", "SOUTHEAST")),
                    "239.70", "71.91", "24.75", 2, "2.40", "0.00", "194.94", 1, "194.94", "0.00", false);

            app.carrier(2210, 150);
            check(app.post("/checkout/summary", body("[{\"sku\":\"MEI-001\",\"quantity\":1}]", "ECONOMY",
                            "FREESHIPPING", "BANK_SLIP", 1, "BRONZE", "SOUTH")),
                    "19.90", "5.97", "22.10", 7, "0.20", "3.49", "39.72", 1, "39.72", "0.00", false);

            app.carrier(5080, 49);
            check(app.post("/checkout/summary", body(
                            "[{\"sku\":\"JAQ-001\",\"quantity\":3},{\"sku\":\"TEN-001\",\"quantity\":1}]",
                            "EXPRESS", null, "PIX", 1, "GOLD", "NORTH")),
                    "1419.60", "0.00", "0.00", 6, "30.00", "-72.48", "1377.12", 1, "1377.12", "70.98", true);
        }
    }

    @Test
    void refusals() {
        try (TestApp app = new TestApp(NOW)) {
            var r = app.post("/checkout/summary", body("[{\"sku\":\"CAM-001\",\"quantity\":1}]", "STORE_PICKUP",
                    "FREESHIPPING", "PIX", 1, "SILVER", "SOUTHEAST"));
            assertEquals(422, r.status());
            assertEquals("COUPON_NOT_APPLICABLE", r.body().get("error").asText());

            app.carrierAnswer = java.util.Optional.empty();
            r = app.post("/checkout/summary", body("[{\"sku\":\"TEN-001\",\"quantity\":1}]", "ECONOMY", null, "PIX", 1,
                    "BRONZE", "NORTH"));
            assertEquals(422, r.status());
            assertEquals("SHIPPING_METHOD_UNAVAILABLE", r.body().get("error").asText());

            app.carrierDown = true;
            r = app.post("/checkout/summary", body("[{\"sku\":\"TEN-001\",\"quantity\":1}]", "EXPRESS", null, "PIX", 1,
                    "BRONZE", "NORTH"));
            assertEquals(503, r.status());
            assertEquals("CARRIER_UNAVAILABLE", r.body().get("error").asText());

            r = app.post("/checkout/summary", body("[{\"sku\":\"REL-001\",\"quantity\":1}]", "COURIER", "", "PIX", 1,
                    "BRONZE", "NORTH"));
            assertEquals("PRODUCT_DISCONTINUED", r.body().get("error").asText());

            r = app.post("/checkout/summary", body("[{\"sku\":\"MOC-001\",\"quantity\":2}]", "COURIER", "", "PIX", 1,
                    "BRONZE", "NORTH"));
            assertEquals("INSUFFICIENT_STOCK", r.body().get("error").asText());

            r = app.post("/checkout/summary", body("[{\"sku\":\"MOC-001\",\"quantity\":1}]", "COURIER", "", "PIX", 1,
                    "BRONZE", "NORTH"));
            assertEquals("INVALID_COUPON", r.body().get("error").asText());

            r = app.post("/checkout/summary", "{}");
            assertEquals("INVALID_ORDER", r.body().get("error").asText());
        }
    }
}
