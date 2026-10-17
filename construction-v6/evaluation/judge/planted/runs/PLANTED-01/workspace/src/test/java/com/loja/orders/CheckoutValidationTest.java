package com.loja.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.loja.orders.support.IntegrationTest;

class CheckoutValidationTest extends IntegrationTest {

    private static final String VALID_REST = "\"zipCode\": \"01310100\", \"region\": \"SOUTH\", "
            + "\"shippingMethod\": \"STORE_PICKUP\", \"paymentMethod\": \"PIX\", \"clubTier\": \"BRONZE\"";
    private static final String CUSTOMER = "\"customer\": { \"cpf\": \"12345678909\", \"email\": \"a@b.com\" }";

    private void expectError(ResultActions result, int status, String code) throws Exception {
                                                                                                              
    }

    private ResultActions raw(String json) throws Exception {
        return summary(json);
    }

    @Test
    void invalidCartOrCustomer() throws Exception {
        String[] bodies = {
            "{" + CUSTOMER + ", \"items\": [], " + VALID_REST + "}",
            "{" + CUSTOMER + ", " + VALID_REST + "}",
            "{" + CUSTOMER + ", \"items\": [{ \"quantity\": 1 }], " + VALID_REST + "}",
            "{" + CUSTOMER + ", \"items\": [{ \"sku\": \"CAM-001\", \"quantity\": 1 }, "
                    + "{ \"sku\": \"CAM-001\", \"quantity\": 2 }], " + VALID_REST + "}",
            "{" + CUSTOMER + ", \"items\": [{ \"sku\": \"CAM-001\", \"quantity\": 0 }], " + VALID_REST + "}",
            "{" + CUSTOMER + ", \"items\": [{ \"sku\": \"CAM-001\", \"quantity\": -1 }], " + VALID_REST + "}",
            "{" + CUSTOMER + ", \"items\": [{ \"sku\": \"CAM-001\" }], " + VALID_REST + "}",
            "{" + CUSTOMER + ", \"items\": [{ \"sku\": \"CAM-001\", \"quantity\": 1.5 }], " + VALID_REST + "}",
            "{\"customer\": { \"email\": \"a@b.com\" }, \"items\": [{ \"sku\": \"CAM-001\", \"quantity\": 1 }], "
                    + VALID_REST + "}",
            "{\"customer\": { \"cpf\": \"12345678909\" }, \"items\": [{ \"sku\": \"CAM-001\", \"quantity\": 1 }], "
                    + VALID_REST + "}",
            "{\"items\": [{ \"sku\": \"CAM-001\", \"quantity\": 1 }], " + VALID_REST + "}",
            "not json",
            "",
        };
        for (String body : bodies) {
            expectError(raw(body), 422, "INVALID_ORDER");
        }
    }

    @Test
    void invalidOrderComesBeforeEverythingElse() throws Exception {
        expectError(raw("{" + CUSTOMER + ", \"items\": [{ \"sku\": \"NOPE\", \"quantity\": 0 }], "
                + "\"region\": \"MARS\"}"), 422, "INVALID_ORDER");
    }

    @Test
    void productChecks() throws Exception {
        expectError(summary(checkout().item("CAM-001", 1).item("XXX-999", 1).json()), 422, "UNKNOWN_PRODUCT");
        expectError(summary(checkout().item("REL-001", 1).item("XXX-999", 1).json()), 422, "UNKNOWN_PRODUCT");
        expectError(summary(checkout().item("BON-001", 1).item("REL-001", 1).json()), 422, "PRODUCT_DISCONTINUED");
        expectError(summary(checkout().item("BON-001", 1).json()), 422, "INSUFFICIENT_STOCK");
        expectError(summary(checkout().item("JAQ-001", 4).json()), 422, "INSUFFICIENT_STOCK");
        summary(checkout().item("JAQ-001", 3).json())                           ;
    }

    @Test
    void fieldChecksInOrder() throws Exception {
        expectError(summary(checkout().item("CAM-001", 1).tier("PLATINUM").region("MARS").json()), 422,
                "INVALID_CLUB_TIER");
        expectError(summary(checkout().item("CAM-001", 1).tier(null).json()), 422, "INVALID_CLUB_TIER");
        expectError(summary(checkout().item("CAM-001", 1).tier("gold").json()), 422, "INVALID_CLUB_TIER");
        expectError(summary(checkout().item("CAM-001", 1).region("MARS").zipCode("x").json()), 422,
                "INVALID_REGION");
        expectError(summary(checkout().item("CAM-001", 1).region(null).json()), 422, "INVALID_REGION");
        expectError(summary(checkout().item("CAM-001", 1).zipCode("01310-100").shipping("PLANE").json()), 422,
                "INVALID_ZIP_CODE");
        expectError(summary(checkout().item("CAM-001", 1).zipCode("1310100").json()), 422, "INVALID_ZIP_CODE");
        expectError(summary(checkout().item("CAM-001", 1).zipCode(null).json()), 422, "INVALID_ZIP_CODE");
        expectError(summary(checkout().item("CAM-001", 1).shipping("PLANE").coupon("NOPE").json()), 422,
                "INVALID_SHIPPING_METHOD");
        expectError(summary(checkout().item("CAM-001", 1).shipping(null).json()), 422, "INVALID_SHIPPING_METHOD");
        expectError(summary(checkout().item("CAM-001", 1).coupon("NOPE").payment("CASH").json()), 422,
                "INVALID_COUPON");
        expectError(summary(checkout().item("CAM-001", 1).coupon("").json()), 422, "INVALID_COUPON");
        expectError(summary(checkout().item("CAM-001", 1).coupon("welcome10").json()), 422, "INVALID_COUPON");
        expectError(summary(checkout().item("CAM-001", 1).coupon("MINUS50").payment("CASH").json()), 422,
                "COUPON_NOT_APPLICABLE");
        expectError(summary(checkout().item("CAM-001", 1).payment("CASH").json()), 422, "INVALID_PAYMENT_METHOD");
        expectError(summary(checkout().item("CAM-001", 1).payment(null).json()), 422, "INVALID_PAYMENT_METHOD");
        expectError(summary(checkout().item("CAM-001", 1).payment("PIX", 2).json()), 422, "INVALID_INSTALLMENTS");
        expectError(summary(checkout().item("CAM-001", 1).payment("BANK_SLIP", 2).json()), 422,
                "INVALID_INSTALLMENTS");
        expectError(summary(checkout().item("CAM-001", 1).payment("CREDIT_CARD", 13).json()), 422,
                "INVALID_INSTALLMENTS");
        expectError(summary(checkout().item("CAM-001", 1).payment("CREDIT_CARD", 0).json()), 422,
                "INVALID_INSTALLMENTS");
        summary(checkout().item("CAM-001", 1).payment("CREDIT_CARD", 12).json())                           ;
    }

    @Test
    void courierUpTo5Kg() throws Exception {
        // 4 tênis = 4,80 kg; mais 1 camiseta = 5,10 kg
        summary(checkout().item("TEN-001", 4).shipping("COURIER").json())                           ;
        summary(checkout().item("TEN-001", 4).item("MEI-001", 2).shipping("COURIER").json())
                                           ;
        expectError(summary(checkout().item("TEN-001", 4).item("CAM-001", 1).shipping("COURIER").json()), 422,
                "SHIPPING_METHOD_UNAVAILABLE");
    }

    @Test
    void carrierOutOfService() throws Exception {
        String request = checkout().item("CAM-001", 1).shipping("ECONOMY").coupon("NOPE").json();
        translog.respond(500, "{}");
        expectError(summary(request), 503, "CARRIER_UNAVAILABLE");
        translog.respond(200, "{ \"error\": \"SOMETHING_ELSE\" }");
        expectError(summary(request), 503, "CARRIER_UNAVAILABLE");
        translog.respond(200, "<html>");
        expectError(summary(request), 503, "CARRIER_UNAVAILABLE");
        translog.respond(200, "{ \"price_cents\": 100 }");
        expectError(summary(request), 503, "CARRIER_UNAVAILABLE");
    }

    @Test
    void carrierTooSlow() throws Exception {
        translog.delay(2500);
        long start = System.nanoTime();
        expectError(summary(checkout().item("CAM-001", 1).shipping("EXPRESS").json()), 503, "CARRIER_UNAVAILABLE");
                                                                             
    }

    @Test
    void carrierAnswersWithinTwoSeconds() throws Exception {
        translog.delay(1000);
        translog.quote(1000, 1);
        summary(checkout().item("CAM-001", 1).shipping("EXPRESS").json())
                                           
                                                               ;
    }

    @Test
    void regionalDispatchDaysOnlyForCarriers() throws Exception {
        translog.quote(1000, 48);
        summary(checkout().item("CAM-001", 1).shipping("ECONOMY").region("NORTHEAST").json())
                                                               ;
        summary(checkout().item("CAM-001", 1).shipping("STORE_PICKUP").region("NORTH").json())
                                                               ;
        summary(checkout().item("CAM-001", 1).shipping("COURIER").region("NORTH").json())
                                                               ;
    }

    @Test
    void bankSlipLimitIsOnOrderTotal() throws Exception {
        // 4 tênis = 999,60 + seguro 1% (10,00) = 1.009,60
        expectError(summary(checkout().item("TEN-001", 4).payment("BANK_SLIP").json()), 422,
                "PAYMENT_METHOD_UNAVAILABLE");
        // com MINUS50: 949,60 + 10,00 = 959,60
        summary(checkout().item("TEN-001", 4).coupon("MINUS50").payment("BANK_SLIP").json())
                                           
                                                                  ;
    }

    @Test
    void pixDiscountOnlyWithoutCoupon() throws Exception {
        // 1 camiseta: 79,90 + seguro 0,80 = 80,70; Pix 5% = 4,04 (4,035 meio para o par)
        summary(checkout().item("CAM-001", 1).json())
                                                                        
                                                                 ;
        // BUY3PAY2 sem 3 unidades: vale, desconto zero, e tira o desconto do Pix
        summary(checkout().item("CAM-001", 1).coupon("BUY3PAY2").json())
                                                                   
                                                                      
                                                                 ;
    }

    @Test
    void goldPaysCourierButNotCarrier() throws Exception {
        summary(checkout().item("CAM-001", 1).shipping("COURIER").tier("GOLD").json())
                                                                  ;
        translog.quote(3000, 24);
        summary(checkout().item("CAM-001", 1).shipping("ECONOMY").tier("GOLD").coupon("FREESHIPPING").json())
                                                                 
                                                                   ;
    }

    @Test
    void minus50NeedsAtLeast300InProducts() throws Exception {
        expectError(summary(checkout().item("MEI-001", 15).coupon("MINUS50").json()), 422, "COUPON_NOT_APPLICABLE");
        summary(checkout().item("MEI-001", 16).coupon("MINUS50").json())
                                           
                                                                    ;
    }
}
