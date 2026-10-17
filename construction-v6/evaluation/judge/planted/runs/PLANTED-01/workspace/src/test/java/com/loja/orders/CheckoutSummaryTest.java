package com.loja.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.loja.orders.support.IntegrationTest;

                                              
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
                                           
                                                                                                                    
                                                  ;
                                                                                                 
    }

    @Test
    void example2_creditCardWithInterest() throws Exception {
        translog.quote(1560, 168);
        summary(checkout().item("CAM-001", 2).item("TEN-001", 1).shipping("ECONOMY").payment("CREDIT_CARD", 6)
                .tier("SILVER").region("MIDWEST").json())
                                           
                                                                                                                   
                                                 ;
                                                                                                 
    }

    @Test
    void example3_courierBankSlipGold() throws Exception {
        summary(checkout().item("FON-001", 2).shipping("COURIER").coupon("MINUS50").payment("BANK_SLIP")
                .tier("GOLD").region("NORTHEAST").json())
                                           
                                                                                                                   
                                                   ;
    }

    @Test
    void example4_buy3pay2StorePickup() throws Exception {
        summary(checkout().item("MEI-001", 7).item("CAM-001", 2).shipping("STORE_PICKUP").coupon("BUY3PAY2")
                .payment("CREDIT_CARD", 3).tier("SILVER").region("SOUTH").json())
                                           
                                                                                                                  
                                                 ;
    }

    @Test
    void example5_couponCappedAt30Percent() throws Exception {
        translog.quote(2475, 30);
        summary(checkout().item("CAM-001", 3).shipping("EXPRESS").coupon("BUY3PAY2").payment("PIX").json())
                                           
                                                                                                                   
                                                  ;
    }

    @Test
    void example6_freeShippingCapped() throws Exception {
        translog.quote(2210, 150);
        summary(checkout().item("MEI-001", 1).shipping("ECONOMY").coupon("FREESHIPPING").payment("BANK_SLIP")
                .region("SOUTH").json())
                                           
                                                                                                                
                                                 ;
    }

    @Test
    void example7_goldPixGift() throws Exception {
        translog.quote(5080, 49);
        summary(checkout().item("JAQ-001", 3).item("TEN-001", 1).shipping("EXPRESS").payment("PIX").tier("GOLD")
                .region("NORTH").json())
                                           
                                                                                                                      
                                                   ;
    }

    @Test
    void example8_freeShippingNotForStorePickup() throws Exception {
        summary(checkout().item("CAM-001", 1).shipping("STORE_PICKUP").coupon("FREESHIPPING").payment("PIX")
                .tier("SILVER").json())
                                                             
                                                                                         ;
    }

    @Test
    void example9_welcomeCouponOnlyOnFirstPurchase() throws Exception {
        String previous = placeOrder(checkout().item("MEI-001", 1).json());
        pay(previous, "TX-1", Instant.now().toString())                           ;

        translog.quote(2900, 36);
        summary(checkout().item("CAM-001", 2).shipping("EXPRESS").coupon("WELCOME10").payment("CREDIT_CARD", 2)
                .region("SOUTH").json())
                                                             
                                                                                         ;
    }

    @Test
    void welcomeCouponStillValidWhenOnlyPreviousOrderWasCancelled() throws Exception {
        String previous = placeOrder(checkout().item("MEI-001", 1).json());
        pay(previous, "TX-1", Instant.now().toString())                           ;
        postJson("/orders/" + previous + "/cancellation", "")                           ;

        summary(checkout().item("CAM-001", 1).coupon("WELCOME10").json())                           ;
    }

    @Test
    void example10_zipNotServed() throws Exception {
        translog.respond(200, "{ \"error\": \"ZIP_NOT_SERVED\" }");
        summary(checkout().item("TEN-001", 1).shipping("ECONOMY").payment("PIX").region("NORTH").json())
                                                             
                                                                                               ;
    }
}
