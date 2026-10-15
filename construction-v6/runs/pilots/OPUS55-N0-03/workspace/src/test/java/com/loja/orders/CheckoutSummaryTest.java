package com.loja.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.loja.orders.support.IntegrationTest;

/** Os exemplos conferidos pelo financeiro. */
class CheckoutSummaryTest extends IntegrationTest {

    private static String summaryJson(String subtotal, String coupon, String shipping, int days, String insurance,
            String adjustment, String finalTotal, int installments, String installmentAmount, String credit,
            boolean gift) {
        return "{\"productsSubtotal\":" + subtotal + ",\"couponDiscount\":" + coupon + ",\"shippingCost\":" + shipping
                + ",\"deliveryDays\":" + days + ",\"insurance\":" + insurance + ",\"paymentAdjustment\":" + adjustment
                + ",\"finalTotal\":" + finalTotal + ",\"installments\":" + installments + ",\"installmentAmount\":"
                + installmentAmount + ",\"nextPurchaseCredit\":" + credit + ",\"gift\":" + gift + "}";
    }

    @Test
    void example1_welcomeCouponExpressNorth() throws Exception {
        translog.quote(3310, 41);
        summary(checkout().item("CAM-001", 2).item("TEN-001", 1).shipping("EXPRESS").coupon("WELCOME10")
                .payment("PIX").tier("BRONZE").region("NORTH").json())
                .andExpect(status().isOk())
                .andExpect(content().string(summaryJson("409.70", "40.97", "33.10", 5, "10.24", "0.00", "412.07", 1,
                        "412.07", "0.00", false)));
        assertThat(translog.queries()).containsExactly("zip=01310100&weight_g=1800&service=EXP");
    }

    @Test
    void example2_creditCardWithInterest() throws Exception {
        translog.quote(1560, 168);
        summary(checkout().item("CAM-001", 2).item("TEN-001", 1).shipping("ECONOMY").payment("CREDIT_CARD", 6)
                .tier("SILVER").region("MIDWEST").json())
                .andExpect(status().isOk())
                .andExpect(content().string(summaryJson("409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6,
                        "77.00", "8.19", false)));
        assertThat(translog.queries()).containsExactly("zip=01310100&weight_g=1800&service=STD");
    }

    @Test
    void example3_courierBankSlipGold() throws Exception {
        summary(checkout().item("FON-001", 2).shipping("COURIER").coupon("MINUS50").payment("BANK_SLIP")
                .tier("GOLD").region("NORTHEAST").json())
                .andExpect(status().isOk())
                .andExpect(content().string(summaryJson("399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1,
                        "379.29", "19.99", false)));
    }

    @Test
    void example4_buy3pay2StorePickup() throws Exception {
        summary(checkout().item("MEI-001", 7).item("CAM-001", 2).shipping("STORE_PICKUP").coupon("BUY3PAY2")
                .payment("CREDIT_CARD", 3).tier("SILVER").region("SOUTH").json())
                .andExpect(status().isOk())
                .andExpect(content().string(summaryJson("299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3,
                        "87.43", "5.98", false)));
    }

    @Test
    void example5_couponCappedAt30Percent() throws Exception {
        translog.quote(2475, 30);
        summary(checkout().item("CAM-001", 3).shipping("EXPRESS").coupon("BUY3PAY2").payment("PIX").json())
                .andExpect(status().isOk())
                .andExpect(content().string(summaryJson("239.70", "71.91", "24.75", 2, "2.40", "0.00", "194.94", 1,
                        "194.94", "0.00", false)));
    }

    @Test
    void example6_freeShippingCapped() throws Exception {
        translog.quote(2210, 150);
        summary(checkout().item("MEI-001", 1).shipping("ECONOMY").coupon("FREESHIPPING").payment("BANK_SLIP")
                .region("SOUTH").json())
                .andExpect(status().isOk())
                .andExpect(content().string(summaryJson("19.90", "5.97", "22.10", 7, "0.20", "3.49", "39.72", 1,
                        "39.72", "0.00", false)));
    }

    @Test
    void example7_goldPixGift() throws Exception {
        translog.quote(5080, 49);
        summary(checkout().item("JAQ-001", 3).item("TEN-001", 1).shipping("EXPRESS").payment("PIX").tier("GOLD")
                .region("NORTH").json())
                .andExpect(status().isOk())
                .andExpect(content().string(summaryJson("1419.60", "0.00", "0.00", 6, "30.00", "-72.48", "1377.12", 1,
                        "1377.12", "70.98", true)));
    }

    @Test
    void example8_freeShippingNotForStorePickup() throws Exception {
        summary(checkout().item("CAM-001", 1).shipping("STORE_PICKUP").coupon("FREESHIPPING").payment("PIX")
                .tier("SILVER").json())
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"error\":\"COUPON_NOT_APPLICABLE\"}", true));
    }

    @Test
    void example9_welcomeCouponOnlyOnFirstPurchase() throws Exception {
        String previous = placeOrder(checkout().item("MEI-001", 1).json());
        pay(previous, "TX-1", Instant.now().toString()).andExpect(status().isOk());

        translog.quote(2900, 36);
        summary(checkout().item("CAM-001", 2).shipping("EXPRESS").coupon("WELCOME10").payment("CREDIT_CARD", 2)
                .region("SOUTH").json())
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"error\":\"COUPON_NOT_APPLICABLE\"}", true));
    }

    @Test
    void welcomeCouponStillValidWhenOnlyPreviousOrderWasCancelled() throws Exception {
        String previous = placeOrder(checkout().item("MEI-001", 1).json());
        pay(previous, "TX-1", Instant.now().toString()).andExpect(status().isOk());
        postJson("/orders/" + previous + "/cancellation", "").andExpect(status().isOk());

        summary(checkout().item("CAM-001", 1).coupon("WELCOME10").json()).andExpect(status().isOk());
    }

    @Test
    void example10_zipNotServed() throws Exception {
        translog.respond(200, "{ \"error\": \"ZIP_NOT_SERVED\" }");
        summary(checkout().item("TEN-001", 1).shipping("ECONOMY").payment("PIX").region("NORTH").json())
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"error\":\"SHIPPING_METHOD_UNAVAILABLE\"}", true));
    }
}
